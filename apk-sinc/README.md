# APK SINC — Monitoramento de Servidores

Sistema de monitoramento de servidores com notificações push, composto por um
**backend** (Python/FastAPI) que faz o monitoramento de verdade e detecta
mudanças de status, e um **aplicativo Android** (Kotlin/Jetpack Compose) que
exibe o status, o histórico e recebe notificações via Firebase Cloud
Messaging — mesmo com o app fechado.

> Preparado para evoluir futuramente para **Omega Monitor** (troca de nome/
> marca sem mudança de arquitetura — ver `docs/architecture.md`).

## Arquitetura (resumo)

```
Fonte de monitoramento -> Backend (polling + máquina de estados) -> Supabase (PostgreSQL) -> FCM -> App Android
```

O Android **nunca** monitora sozinho: ele é cliente da API do backend e
receptor passivo de notificações push. Toda a lógica de detecção de
mudança de status, anti-spam e histórico vive no backend. O **Supabase**
(PostgreSQL) é o banco principal — armazena unidades monitoradas, histórico
de eventos, dispositivos e configurações; o backend continua sendo o único
responsável por consultar a fonte real e aplicar a regra de negócio (o
Supabase nunca substitui o monitor). Detalhes completos em
[`docs/architecture.md`](docs/architecture.md).

## Estrutura do projeto

```
apk-sinc/
├── android/                    # App Android (Kotlin + Jetpack Compose + MVVM)
├── backend/                    # API + monitoramento (Python + FastAPI)
│   ├── app/                    # código do backend (monitor, services, database, routers)
│   ├── supabase/migrations/    # schema SQL do banco principal (Supabase/PostgreSQL)
│   └── tests/                  # testes automatizados (sempre via SQLite, nunca Supabase real)
├── docs/                       # Documentação técnica
└── README.md
```

## Início rápido

### Backend

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env          # SOURCE_MODE=mock, DB_BACKEND=supabase por padrão
uvicorn app.main:app --reload
```

Para configurar o Supabase (obrigatório em produção), veja
[`docs/setup-supabase.md`](docs/setup-supabase.md). Para desenvolver sem um
projeto Supabase, use `DB_BACKEND=sqlite` no `.env`. Veja
[`docs/setup-backend.md`](docs/setup-backend.md) para detalhes gerais.

### Testes do backend

```bash
cd backend
.venv/bin/python -m pytest -v
```

37 testes cobrindo a máquina de estados (baseada em tempo decorrido),
anti-spam, o repository (SQLite, mesmo contrato do Supabase), parsing da
fonte real (JSON e HTML), o ciclo completo de monitoramento e a API. Nenhum
teste depende de um Supabase real. Ver [`docs/testing.md`](docs/testing.md).

### Android

Abra a pasta `android/` no Android Studio (Koala ou mais recente),
configure `google-services.json` (ver
[`docs/setup-firebase.md`](docs/setup-firebase.md)) e rode
`./gradlew assembleDebug`. Detalhes em
[`docs/build-android.md`](docs/build-android.md).

## Status importante sobre a fonte de dados real

A URL de monitoramento real
(`http://cli-1237.ddns.a7cloud.net.br:8080/online/monitorsincronizacao/`)
**não pôde ser inspecionada a partir deste ambiente de desenvolvimento** —
não há rota de rede até esse host (porta 8080 / HTTP puro, bloqueado pela
política de egress do sandbox usado para construir o projeto). Isso está
documentado em detalhe em `docs/architecture.md` (seção 1), junto com o
plano de ação: rodar `backend/scripts/inspect_source.py` a partir de um
ambiente com acesso e ajustar `backend/app/monitor/real_source.py` conforme
o formato real encontrado. Até lá, `SOURCE_MODE=mock` permite desenvolver e
testar o sistema inteiro.

## Regras de negócio principais

- Cada unidade (`sync_units`, identificada por código A7) tem `send_status`
  e `receive_status` calculados pelo tempo decorrido desde o último
  envio/recebimento, comparado a limites configuráveis
  (`monitor_settings.warning_threshold_minutes` / `critical_threshold_minutes`,
  padrão 5 / 15 minutos). `overall_status` é o pior entre os dois:
  `NORMAL` → `ATENÇÃO` → `CRÍTICO`.
- Notificação push disparada **apenas** quando `overall_status` muda de
  valor entre um ciclo e outro — nunca ao repetir o mesmo status
  (anti-spam).
- Histórico completo de eventos por unidade (`sync_events`), com duração
  da indisponibilidade calculada automaticamente na resolução.
- Para o app Android, esse domínio é traduzido para o vocabulário legado
  `ONLINE`/`ATENÇÃO`/`OFFLINE` nos endpoints `/api/servers` e
  `/api/events`, sem exigir nenhuma mudança no app.

## Segurança

Nenhuma credencial (Supabase `SERVICE_ROLE_KEY`, Firebase Admin SDK,
`google-services.json`, tokens) é commitada — todas ficam fora do controle
de versão via `.gitignore` e são fornecidas por variáveis de ambiente /
arquivos locais. A `SERVICE_ROLE_KEY` do Supabase existe **somente no
backend** e nunca é enviada ao app Android, que fala apenas com a API
FastAPI. Ver `backend/.env.example`, `docs/setup-supabase.md` e
`docs/setup-firebase.md`.

## Documentação

- [`docs/architecture.md`](docs/architecture.md) — arquitetura completa e investigação da fonte
- [`docs/setup-backend.md`](docs/setup-backend.md) — instalação e configuração do backend
- [`docs/setup-supabase.md`](docs/setup-supabase.md) — criação do projeto Supabase, migrations e `.env`
- [`docs/setup-firebase.md`](docs/setup-firebase.md) — configuração do Firebase Cloud Messaging
- [`docs/build-android.md`](docs/build-android.md) — build do app e geração do APK
- [`docs/testing.md`](docs/testing.md) — como rodar e o que cobrem os testes
