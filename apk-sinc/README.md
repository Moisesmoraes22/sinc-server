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
Fonte de monitoramento -> Backend (polling + máquina de estados) -> FCM -> App Android
```

O Android **nunca** monitora sozinho: ele é cliente da API do backend e
receptor passivo de notificações push. Toda a lógica de detecção de queda/
retorno, anti-spam e histórico vive no backend. Detalhes completos em
[`docs/architecture.md`](docs/architecture.md).

## Estrutura do projeto

```
apk-sinc/
├── android/     # App Android (Kotlin + Jetpack Compose + MVVM)
├── backend/     # API + monitoramento (Python + FastAPI)
├── docs/        # Documentação técnica
└── README.md
```

## Início rápido

### Backend

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env          # SOURCE_MODE=mock por padrão
uvicorn app.main:app --reload
```

Veja [`docs/setup-backend.md`](docs/setup-backend.md) para detalhes.

### Testes do backend

```bash
cd backend
.venv/bin/python -m pytest -v
```

22 testes cobrindo a máquina de estados, anti-spam, parsing da fonte real
(JSON e HTML) e o ciclo completo de monitoramento. Ver
[`docs/testing.md`](docs/testing.md).

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

- Status: `ONLINE` → `ATENÇÃO` (1ª falha) → `ATENÇÃO` (2ª falha) →
  `OFFLINE` (3ª falha consecutiva). Configurável via `.env`.
- Notificação push disparada **apenas** nas transições `* → OFFLINE` e
  `OFFLINE → ONLINE` — nunca em falhas isoladas ou repetidas enquanto já
  offline (anti-spam).
- Histórico completo de eventos por servidor, com duração da indisponibilidade.

## Segurança

Nenhuma credencial (Firebase Admin SDK, `google-services.json`, tokens) é
commitada — todas ficam fora do controle de versão via `.gitignore` e são
fornecidas por variáveis de ambiente / arquivos locais. Ver
`backend/.env.example` e `docs/setup-firebase.md`.

## Documentação

- [`docs/architecture.md`](docs/architecture.md) — arquitetura completa e investigação da fonte
- [`docs/setup-backend.md`](docs/setup-backend.md) — instalação e configuração do backend
- [`docs/setup-firebase.md`](docs/setup-firebase.md) — configuração do Firebase Cloud Messaging
- [`docs/build-android.md`](docs/build-android.md) — build do app e geração do APK
- [`docs/testing.md`](docs/testing.md) — como rodar e o que cobrem os testes
