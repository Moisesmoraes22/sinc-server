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

## 2. Como os servidores e status serão identificados

Independente do formato real (JSON ou HTML), o backend normaliza toda leitura
da fonte para uma estrutura interna única:

```python
class ServerReading:
    external_id: str      # identificador estável vindo da fonte (nome ou id)
    name: str              # nome de exibição
    raw_status: str        # status bruto como veio da fonte (ex: "online", "OK", "1")
    response_time_ms: int | None
    checked_at: datetime
```

O `external_id` é usado como chave de identidade do servidor no banco local
(tabela `servers`). Se a fonte não fornecer um ID explícito, usamos o nome
normalizado (slug) como `external_id` — estável desde que o nome não mude.

## 3. Estratégia de status (máquina de estados)

Regra de negócio (configurável via `.env`):

- `FAILURE_THRESHOLD_ATTENTION = 1` (1ª falha → ATENÇÃO)
- `FAILURE_THRESHOLD_OFFLINE = 3` (3ª falha consecutiva → OFFLINE)

Transições:

```
ONLINE --(falha 1)--> ATENÇÃO --(falha 2)--> ATENÇÃO --(falha 3)--> OFFLINE
OFFLINE --(sucesso)--> ONLINE
ATENÇÃO --(sucesso)--> ONLINE
```

O contador de falhas consecutivas (`consecutive_failures`) é resetado a zero
em qualquer sucesso. Isso evita falso positivo de uma única falha de rede
momentânea virar "servidor offline".

Notificação FCM só é disparada nas transições:
- `ONLINE|ATENÇÃO → OFFLINE` (perda real, 3ª falha)
- `OFFLINE → ONLINE` (recuperação)

Ou seja, entrar em ATENÇÃO **não** notifica (evita spam em flapping leve);
apenas o estado terminal OFFLINE e o retorno a ONLINE notificam. Isso está
implementado em `backend/app/services/state_machine.py` e é a peça central
anti-spam: o disparo de notificação depende exclusivamente da comparação
`status_anterior != status_novo` no nível "notificável", nunca do resultado
bruto de cada checagem individual.

## 4. Arquitetura do backend

```
backend/app/
├── main.py                 # FastAPI app, scheduler de polling
├── config.py                # Settings via pydantic-settings + .env
├── monitor/
│   ├── source_client.py     # Protocol/interface comum
│   ├── real_source.py       # cliente HTTP real (httpx) + parser JSON/HTML
│   ├── mock_source.py       # cliente simulado com cenários controláveis
│   └── poller.py            # loop assíncrono de coleta periódica
├── services/
│   ├── state_machine.py     # regra de transição de estado + anti-spam
│   └── monitor_service.py   # orquestra: fetch -> compare -> persist -> notify
├── notifications/
│   └── fcm_client.py        # Firebase Admin SDK, envio de push
├── database/
│   ├── db.py                 # engine SQLite (SQLAlchemy)
│   └── repository.py         # CRUD de servers/events/tokens
├── models/
│   ├── orm.py                 # tabelas SQLAlchemy
│   └── schemas.py             # Pydantic (request/response da API)
└── routers/
    ├── servers.py             # GET /api/servers, /api/servers/{id}
    ├── events.py               # GET /api/events
    ├── health.py                # GET /api/health
    ├── devices.py                # POST /api/devices (registro de FCM token)
    └── mock.py                    # POST /api/mock/scenario (apenas em SOURCE_MODE=mock)
```

Loop de monitoramento (`poller.py`): a cada `POLL_INTERVAL_SECONDS` (padrão 30s),
busca leituras da fonte configurada, com timeout e retry (backoff simples,
`RETRY_ATTEMPTS`, `RETRY_BACKOFF_SECONDS`). Erros de rede na fonte inteira
(não apenas de um servidor) são tratados como falha "no ar" e logados, sem
derrubar o processo — o loop continua na próxima iteração.

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
Poller (30s) -> SourceClient.fetch_servers()
             -> StateMachine.evaluate(previous, reading)
             -> se houve transição notificável:
                  -> Repository.save_event(...)
                  -> FcmClient.send(topic="sinc-alerts", title, body, data={serverId, status})
             -> Repository.upsert_server_state(...)
```

O app assina o tópico FCM `sinc-alerts` (ou recebe por token individual,
conforme configuração) e, ao tocar na notificação, navega direto para a tela
de detalhes do servidor usando `serverId` do payload `data`.

## 7. Estrutura de dados (banco local do backend)

```sql
servers(
  id TEXT PRIMARY KEY,        -- external_id normalizado
  name TEXT,
  status TEXT,                 -- ONLINE | ATENCAO | OFFLINE
  consecutive_failures INTEGER,
  response_time_ms INTEGER,
  last_check TIMESTAMP,
  last_down_at TIMESTAMP NULL,
  last_up_at TIMESTAMP NULL
)

events(
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  server_id TEXT,
  from_status TEXT,
  to_status TEXT,
  occurred_at TIMESTAMP,
  reason TEXT NULL,
  response_time_ms INTEGER NULL
)

device_tokens(
  token TEXT PRIMARY KEY,
  platform TEXT,
  registered_at TIMESTAMP
)
```

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
