# Setup do Backend

## Requisitos

- Python 3.11+
- pip / venv

## Instalação

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate      # Windows: .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
```

## Configuração (`.env`)

| Variável | Descrição | Padrão |
|---|---|---|
| `SOURCE_MODE` | `mock` ou `real` | `mock` |
| `SOURCE_URL` | URL da fonte real de monitoramento | `http://cli-1237.ddns.a7cloud.net.br:8080/online/monitorsincronizacao/` |
| `SOURCE_TIMEOUT_SECONDS` | Timeout da requisição à fonte | `10` |
| `POLL_INTERVAL_SECONDS` | Intervalo entre checagens | `30` |
| `RETRY_ATTEMPTS` | Tentativas por ciclo em caso de erro | `3` |
| `RETRY_BACKOFF_SECONDS` | Backoff entre tentativas | `2` |
| `FAILURE_THRESHOLD_ATTENTION` | Falhas consecutivas para ATENÇÃO | `1` |
| `FAILURE_THRESHOLD_OFFLINE` | Falhas consecutivas para OFFLINE | `3` |
| `DATABASE_URL` | URL SQLAlchemy | `sqlite:///./sinc.db` |
| `FIREBASE_CREDENTIALS_PATH` | Caminho do service account | `./firebase-adminsdk.json` |
| `FCM_TOPIC` | Tópico FCM usado para broadcast | `sinc-alerts` |
| `FCM_DRY_RUN` | Se `true`, apenas loga em vez de enviar push real | `true` |

## Rodando

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

A API sobe em `http://localhost:8000`. O loop de monitoramento inicia
automaticamente (`app/main.py`, evento de lifespan) e roda em background
dentro do próprio processo assíncrono do FastAPI.

Documentação interativa (Swagger): `http://localhost:8000/docs`.

## Endpoints principais

- `GET /api/servers` — lista todos os servidores e status atual
- `GET /api/servers/{id}` — detalhe de um servidor
- `GET /api/events?server_id=&status=&limit=` — histórico de eventos
- `GET /api/health` — status do backend e do último ciclo de polling
- `POST /api/devices` — registra um FCM token de dispositivo
- `POST /api/mock/scenario` — (apenas `SOURCE_MODE=mock`) força uma
  sequência de resultados `up`/`down` para um servidor, útil para testar o
  app manualmente sem esperar o ciclo real

Exemplo de uso do mock para simular uma queda:

```bash
curl -X POST http://localhost:8000/api/mock/scenario \
  -H "Content-Type: application/json" \
  -d '{"server_id": "server-principal", "sequence": ["down", "down", "down", "up"]}'
```

## Ativando a fonte real

1. Edite `.env`: `SOURCE_MODE=real`.
2. A partir de um ambiente com acesso de rede à URL real, rode:
   ```bash
   python backend/scripts/inspect_source.py
   ```
3. Ajuste `app/monitor/real_source.py` (`_parse_json` / `_parse_html`) para o
   formato exato retornado pela fonte, se necessário.
4. Rode os testes novamente (`pytest`) para garantir que nada quebrou.

## Produção

Para produção, recomenda-se rodar atrás de um processo supervisionado
(systemd, supervisor, Docker) e usar `uvicorn` com múltiplos workers ou
`gunicorn -k uvicorn.workers.UvicornWorker`. O SQLite é suficiente para o
volume esperado (dezenas de servidores); para escalar mais, trocar
`DATABASE_URL` por Postgres é uma mudança de configuração apenas (SQLAlchemy).
