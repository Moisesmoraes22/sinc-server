# Arquitetura — APK SINC

## 1. Investigação da fonte de dados

**URL alvo:** `http://cli-1237.ddns.a7cloud.net.br:8080/online/monitorsincronizacao/`

### 1.1 Resultado da investigação técnica

Foram feitas três tentativas independentes de acesso a partir deste ambiente de
desenvolvimento:

| Método | Resultado |
|---|---|
| `curl` direto (sem proxy) | `Connection timed out` na porta 8080 (IP resolvido: `45.171.234.82`) |
| `curl` via proxy de saída do ambiente (`HTTPS_PROXY`) | `405 Method Not Allowed` — o proxy do ambiente só aceita túneis `CONNECT` HTTPS na porta 443. A URL alvo é **HTTP puro em porta 8080**, que o proxy explicitamente não suporta ("non-443 HTTPS ports" não são suportadas) |
| Ferramenta de fetch de página (rede diferente do proxy local) | `ECONNREFUSED 45.171.234.82:8080` |

**Conclusão:** este ambiente de desenvolvimento (sandbox remoto) não tem rota de
rede para `cli-1237.ddns.a7cloud.net.br:8080`. Isso é uma restrição do
ambiente de execução (política de egress só permite HTTPS/443 para hosts
liberados), não um problema da fonte em si. Não há como inspecionar
headers/HTML/JS reais dessa página a partir daqui.

**Isso não bloqueia o projeto.** O backend foi desenhado para que a fonte real
seja *plugável*: existe um adaptador `RealSourceClient` que faz a chamada HTTP
real (via `httpx`), e será validado quando o backend rodar em um ambiente com
rota até esse host (ex.: a máquina do usuário, uma VPS, ou o próprio servidor
onde a página está hospedada). Até lá, o desenvolvimento segue 100% com a
camada `MockSourceClient`.

### 1.2 Estratégia adotada para não travar o projeto

1. Criar uma interface `SourceClient` (Protocol) com um único método
   `fetch_servers() -> list[ServerReading]`.
2. `RealSourceClient` implementa essa interface fazendo `GET` na URL real e
   tentando, em ordem:
   - Content-Type `application/json` → parse direto como JSON (caso a página
     seja uma API/endpoint JSON, como o nome `.../monitorsincronizacao/`
     sugere ser um endpoint de sincronização e não uma página HTML de
     apresentação);
   - Caso contrário, fallback para parsing de HTML com `BeautifulSoup`,
     procurando por tabelas/listas com padrões de nome+status
     (ONLINE/OFFLINE) — comum em páginas de monitoramento internas simples.
   - Todo o parsing fica isolado em `backend/app/monitor/real_source.py` para
     que, assim que o ambiente real for acessado, seja fácil ajustar o
     parser ao formato exato encontrado (JSON, HTML, ou outro) sem tocar no
     resto do sistema.
3. `MockSourceClient` gera leituras determinísticas e também permite
   cenários controlados via API (`/api/mock/scenario`) para simular quedas,
   flapping, recuperação, etc.
4. A escolha de qual client usar é feita por configuração
   (`SOURCE_MODE=real|mock` no `.env`), nunca por código duplicado.

> **Ação de acompanhamento:** quando alguém com acesso de rede à URL puder
> rodar `curl -v http://cli-1237.ddns.a7cloud.net.br:8080/online/monitorsincronizacao/`
> (ou abrir no navegador e inspecionar via DevTools → Network), o resultado
> deve ser usado para ajustar `real_source.py`. O script
> `backend/scripts/inspect_source.py` incluído neste repositório automatiza
> essa inspeção (status, headers, content-type, corpo, tentativa de parse
> JSON e HTML) e deve ser executado a partir de um ambiente com acesso.

## 2. Como as unidades e status são identificados

Independente do formato real (JSON ou HTML), o backend normaliza toda
leitura da fonte para uma estrutura interna única (`app/models/domain.py`):

```python
@dataclass
class SyncUnitReading:
    a7_code: str                       # identificador estavel e unico vindo da fonte
    business_unit: str                 # nome de exibicao da unidade
    revisions_to_send: int | None      # revisoes pendentes de envio, se informado
    last_send_at: datetime | None      # ultimo envio bem-sucedido
    last_receive_at: datetime | None   # ultimo recebimento bem-sucedido
    checked_at: datetime | None
```

O `a7_code` é usado como chave de identidade da unidade (`sync_units.a7_code`,
`unique`). Se a fonte não fornecer um código explícito, o parser gera um a
partir do nome da unidade normalizado (slug) — estável desde que o nome não
mude. O status (`send_status`/`receive_status`/`overall_status`) **não** vem
pronto da fonte: é calculado pelo backend a partir de `last_send_at` /
`last_receive_at` (ver seção 3).

## 3. Estratégia de status (máquina de estados)

> **Nota de evolução:** a primeira versão do projeto usava uma regra de
> "N falhas consecutivas" (ver histórico do repositório). A partir da
> migração para o domínio de unidades de sincronização (`sync_units`), a
> regra passou a ser baseada em **tempo decorrido desde o último
> envio/recebimento**, que é o que a fonte real (".../monitorsincronizacao/")
> efetivamente mede. O vocabulário de status também mudou de
> `ONLINE/ATENÇÃO/OFFLINE` para `NORMAL/ATENÇÃO/CRÍTICO` — o app Android
> continua recebendo o vocabulário antigo nos endpoints legados
> (`/api/servers`, `/api/events`), traduzido automaticamente pelo backend
> (`app/services/legacy_adapter.py`), então **nenhuma mudança foi
> necessária no app**.

Regra de negócio (configurável em runtime via a tabela `monitor_settings`,
não hardcoded no código — ver `docs/setup-supabase.md`):

- `warning_threshold_minutes` (padrão 5): a partir de quantos minutos sem
  envio/recebimento a unidade entra em `ATENCAO`.
- `critical_threshold_minutes` (padrão 15): a partir de quantos minutos a
  unidade se torna `CRITICO`.

`send_status` e `receive_status` são calculados independentemente (uma
unidade pode estar atrasada só no envio, só no recebimento, ou nos dois);
`overall_status` é sempre o pior dos dois:

```
elapsed < warning_threshold                    -> NORMAL
warning_threshold <= elapsed < critical_threshold -> ATENCAO
elapsed >= critical_threshold                   -> CRITICO

overall_status = pior(send_status, receive_status)
```

Uma unidade sem nenhum envio/recebimento registrado é tratada como
`CRITICO` (pior cenário possível, nunca um falso "tudo bem").

Notificação FCM é disparada **somente quando `overall_status` muda de
valor** entre um ciclo e outro (`NORMAL→ATENCAO`, `ATENCAO→CRITICO`,
`CRITICO→NORMAL`, ou até um salto direto `NORMAL→CRITICO`). Repetir o
mesmo `overall_status` em ciclos seguidos (ex.: `CRITICO→CRITICO`) nunca
gera nova notificação. Isso está implementado em
`backend/app/services/state_machine.py` (`should_notify`) e é a peça
central do anti-spam: o disparo depende exclusivamente da comparação
`overall_status_anterior != overall_status_novo`, nunca do resultado bruto
de uma checagem isolada.

O campo `consecutive_failures` continua existindo em `sync_units` como
contador informativo (quantos ciclos seguidos a unidade não está `NORMAL`),
mas não é mais o que decide o status — quem decide é o tempo decorrido.

## 4. Arquitetura do backend

```
backend/app/
├── main.py                    # FastAPI app, monta repository + poller no lifespan
├── config.py                  # Settings via pydantic-settings + .env
├── dependencies.py            # estado compartilhado (repository, poller) + DI do FastAPI
├── monitor/
│   ├── source_client.py       # Protocol/interface comum (fetch_units)
│   ├── real_source.py         # cliente HTTP real (httpx) + parser JSON/HTML
│   ├── mock_source.py         # cliente simulado com cenários NORMAL/ATENCAO/CRITICO
│   ├── factory.py             # escolhe RealSourceClient x MockSourceClient
│   └── poller.py              # loop assíncrono de coleta periódica
├── services/
│   ├── state_machine.py       # status por tempo decorrido + anti-spam
│   ├── monitor_service.py     # orquestra: fetch -> avaliar -> persistir -> notificar
│   └── legacy_adapter.py      # traduz sync_units/sync_events -> formato legado do app
├── notifications/
│   └── fcm_client.py          # Firebase Admin SDK, envio de push por token + tópico
├── database/
│   ├── base.py                 # Protocol `Repository` (contrato desacoplado de banco)
│   ├── factory.py               # escolhe SupabaseRepository x SqliteRepository (DB_BACKEND)
│   ├── supabase_client.py       # cliente oficial supabase-py (SERVICE_ROLE_KEY)
│   ├── supabase_repository.py   # Repository sobre Supabase/PostgreSQL (producao)
│   ├── sqlite_repository.py     # Repository sobre SQLite/SQLAlchemy (dev local / testes)
│   └── db.py                    # helpers de engine/sessionmaker usados pelo sqlite_repository
├── models/
│   ├── domain.py               # dataclasses de dominio (SyncUnit, SyncEvent, Device, ...)
│   ├── orm.py                  # tabelas SQLAlchemy (schema espelha as migrations Supabase)
│   └── schemas.py              # Pydantic (request/response da API, novo + legado)
└── routers/
    ├── sync_units.py           # GET /api/sync-units, /api/sync-units/{a7_code}
    ├── servers.py               # GET /api/servers, /api/servers/{id} (legado, compat Android)
    ├── events.py                 # GET /api/events (legado, compat Android)
    ├── health.py                  # GET /api/health (database/monitor/firebase)
    ├── devices.py                  # POST/GET /api/devices
    └── mock.py                      # POST /api/mock/scenario (apenas em SOURCE_MODE=mock)
```

Loop de monitoramento (`poller.py`): a cada ciclo, busca leituras da fonte
configurada, com retry (backoff simples, `RETRY_ATTEMPTS`,
`RETRY_BACKOFF_SECONDS`). O intervalo entre ciclos é lido de
`monitor_settings.check_interval_seconds` (via `Repository.get_settings()`)
a cada iteração — mudar esse valor no banco (Supabase ou SQLite) vale sem
reiniciar o processo. Erros de rede na fonte inteira são tratados como
falha e logados, sem derrubar o processo — o loop continua na próxima
iteração.

### 4.1 Persistência: Supabase como banco principal, desacoplado via Repository

A partir desta etapa, **o Supabase (PostgreSQL) é o banco principal em
produção**. Para que a lógica de negócio (`monitor_service.py`,
`state_machine.py`, os routers) nunca dependa diretamente de SQLAlchemy ou
do cliente Supabase, toda persistência passa pela interface `Repository`
(`app/database/base.py` — um `Protocol` com métodos como `get_unit`,
`upsert_unit`, `add_event`, `list_events`, `register_device`,
`list_active_device_tokens`, `get_settings`, `health_check`).

Duas implementações concretas satisfazem esse contrato:

- **`SupabaseRepository`** (`app/database/supabase_repository.py`) — usa o
  cliente oficial `supabase-py`, autenticado com a `SERVICE_ROLE_KEY`
  (privilégios administrativos, ignora RLS). É a implementação usada em
  produção (`DB_BACKEND=supabase`).
- **`SqliteRepository`** (`app/database/sqlite_repository.py`) — usa
  SQLAlchemy sobre um arquivo/`:memory:` SQLite, com um schema
  (`app/models/orm.py`) que espelha exatamente as tabelas do Supabase. É
  usada para desenvolvimento local sem depender de um projeto Supabase
  configurado (`DB_BACKEND=sqlite`) e, principalmente, por **todos** os
  testes automatizados — nenhum teste depende do Supabase real.

`app/database/factory.py` escolhe a implementação em runtime a partir de
`Settings.db_backend`. O restante do backend (`MonitorService`, routers)
recebe o `Repository` já pronto via `app/dependencies.py` e nunca sabe (nem
precisa saber) qual banco está por trás.

A `SUPABASE_SERVICE_ROLE_KEY` existe **somente no processo do backend**,
lida de variável de ambiente (`.env`, nunca commitado) — nunca é enviada ao
app Android. O fluxo permanece:

```
Android -> API FastAPI (backend) -> Supabase (service role)
```

Nunca `Android -> Supabase` diretamente. Ver `docs/setup-supabase.md` para
o passo a passo de criação do projeto, migrations e RLS.

## 5. Arquitetura Android

MVVM com Jetpack Compose:

```
android/app/src/main/java/com/apksinc/monitor/
├── SincApplication.kt
├── data/
│   ├── remote/ (Retrofit: ApiService, DTOs)
│   ├── local/ (Room: ServerEntity, EventEntity, dao)
│   ├── repository/ (ServerRepository)
│   └── fcm/ (SincFirebaseMessagingService)
├── domain/ (models de domínio, use cases simples)
├── ui/
│   ├── dashboard/ (DashboardScreen, DashboardViewModel)
│   ├── details/ (ServerDetailsScreen, ServerDetailsViewModel)
│   ├── history/ (HistoryScreen, HistoryViewModel)
│   ├── settings/ (SettingsScreen, SettingsViewModel)
│   ├── components/ (StatusBadge, ServerCard, cards reutilizáveis)
│   └── theme/ (Color.kt, Theme.kt, Type.kt — Material 3, dark/light)
└── navigation/ (SincNavHost)
```

O app é **cliente** do backend (`GET /api/servers`, `/api/servers/{id}`,
`/api/events`) e **receptor passivo de push** via FCM — nenhuma lógica de
monitoramento roda no app. Isso satisfaz o requisito de que o Android não
seja responsável sozinho pelo monitoramento em segundo plano: mesmo com o
app fechado, o backend detecta a mudança e envia a notificação via FCM, que o
sistema operacional entrega mesmo sem o app em memória (`SincFirebaseMessagingService`
com prioridade alta / canal de notificação dedicado).

## 6. Fluxo de notificações fim-a-fim

```
Poller (check_interval_seconds) -> SourceClient.fetch_units()
             -> MonitorService: calcula send/receive/overall_status (state_machine.evaluate)
             -> Repository.upsert_unit(...)                [Supabase ou SQLite]
             -> se overall_status mudou (should_notify):
                  -> Repository.add_event(...)              [sync_events]
                  -> Repository.list_active_device_tokens()  [devices]
                  -> FcmClient.send_status_change(a7_code, business_unit, novo_status, tokens)
                       -> envia para cada token individualmente + topico "sinc-alerts" (fallback)
```

O app registra seu FCM token via `POST /api/devices` (já implementado desde
a primeira versão, em `SincFirebaseMessagingService.onNewToken`) e, ao
tocar na notificação, navega direto para a tela de detalhes do servidor
usando o `serverId` (= `a7_code`) do payload `data`.

## 7. Estrutura de dados

Fonte da verdade em produção: Supabase/PostgreSQL (migrations em
`backend/supabase/migrations/`). O `SqliteRepository` usado em
desenvolvimento local/testes espelha exatamente este schema
(`backend/app/models/orm.py`).

```sql
sync_units(
  id UUID PRIMARY KEY,
  a7_code TEXT UNIQUE NOT NULL,        -- identificador estavel da unidade
  business_unit TEXT NOT NULL,
  revisions_to_send INTEGER,
  last_send_at TIMESTAMPTZ,
  last_receive_at TIMESTAMPTZ,
  send_elapsed_minutes NUMERIC,
  receive_elapsed_minutes NUMERIC,
  send_status TEXT,                     -- NORMAL | ATENCAO | CRITICO
  receive_status TEXT,                  -- NORMAL | ATENCAO | CRITICO
  overall_status TEXT,                  -- NORMAL | ATENCAO | CRITICO
  consecutive_failures INTEGER,
  last_checked_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ
)

sync_events(
  id BIGINT PRIMARY KEY,
  unit_id UUID REFERENCES sync_units(id),
  a7_code TEXT,
  business_unit TEXT,
  event_type TEXT,        -- STATUS_CHANGE | PROBLEM_STARTED | PROBLEM_RESOLVED
  direction TEXT,          -- SEND | RECEIVE | BOTH
  previous_status TEXT,
  new_status TEXT,
  started_at TIMESTAMPTZ,
  resolved_at TIMESTAMPTZ,
  duration_seconds INTEGER,
  message TEXT,
  created_at TIMESTAMPTZ
)

monitor_settings(
  id SMALLINT PRIMARY KEY,          -- singleton (sempre 1)
  warning_threshold_minutes INTEGER,
  critical_threshold_minutes INTEGER,
  check_interval_seconds INTEGER,
  updated_at TIMESTAMPTZ
)

devices(
  id UUID PRIMARY KEY,
  device_name TEXT,
  fcm_token TEXT UNIQUE NOT NULL,   -- nunca exposto pela API (ver DeviceOut em schemas.py)
  active BOOLEAN,
  created_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ,
  last_seen_at TIMESTAMPTZ
)
```

Índices principais (`002_indexes.sql`): `sync_units.a7_code` (único),
`sync_units.business_unit`, `sync_units.overall_status`,
`sync_units.last_checked_at`, `sync_events.unit_id`,
`sync_events.created_at`, `devices.fcm_token` (único), `devices.active`.

Row Level Security (`003_rls.sql`): habilitado nas quatro tabelas, sem
nenhuma policy permissiva para `anon`/`authenticated` — só a
`service_role` (usada exclusivamente pelo backend) lê/escreve. Detalhado em
`docs/setup-supabase.md`.

## 8. Limitações conhecidas deste ambiente de build

- Não há rota de rede até a fonte real (`cli-1237.ddns.a7cloud.net.br:8080`) —
  documentado acima. O backend deve ser executado/validado contra a fonte
  real em um ambiente com acesso (ex.: máquina local do usuário).
- Não há Android SDK instalado neste sandbox, e o download do SDK
  (`dl.google.com`) é bloqueado pela política de egress do ambiente
  (`403 Forbidden`). Por isso o projeto Android é entregue completo e
  pronto para abrir no Android Studio / `./gradlew assembleDebug` em uma
  máquina com o SDK instalado, mas a geração do `.apk` em si **não pôde ser
  executada dentro deste sandbox**. Isso está detalhado em
  `docs/build-android.md`.
- **Atualização:** um projeto Supabase real (`apk-sinc`,
  ref `ajiavjvynpiqlrggzypk`) foi criado e as 6 migrations foram aplicadas
  e validadas com sucesso (schema, índices, RLS, constraints, anti-spam —
  ver `docs/setup-supabase.md`, seção 9). Isso foi feito via integração MCP
  do Supabase, que não passa pelo proxy de egress deste sandbox. O que
  **não** pôde ser validado a partir daqui é o processo do backend
  (`uvicorn`) conversando com o Supabase via rede normal (HTTPS direto):
  o sandbox bloqueia `*.supabase.co` no proxy de saída (mesma categoria de
  restrição que bloqueia `dl.google.com` e a fonte real na porta 8080).
  `SUPABASE_URL`/`SUPABASE_SERVICE_ROLE_KEY` já estão configurados em
  `backend/.env` (fora do git) — rodar `uvicorn app.main:app` numa máquina
  com egress normal deve funcionar sem nenhuma mudança de código.
