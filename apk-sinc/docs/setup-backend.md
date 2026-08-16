# Setup do Backend

## Requisitos

- Python 3.11+
- pip / venv
- Um projeto Supabase configurado (produção) — ver
  [`docs/setup-supabase.md`](setup-supabase.md) — ou `DB_BACKEND=sqlite`
  para desenvolvimento local rápido sem Supabase.

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
| `RETRY_ATTEMPTS` | Tentativas por ciclo em caso de erro | `3` |
| `RETRY_BACKOFF_SECONDS` | Backoff entre tentativas | `2` |
| `DB_BACKEND` | `supabase` (produção) ou `sqlite` (dev local) | `supabase` |
| `SUPABASE_URL` | URL do projeto Supabase | — |
| `SUPABASE_SERVICE_ROLE_KEY` | Chave administrativa (nunca vai para o Android) | — |
| `SQLITE_DATABASE_URL` | Usado apenas com `DB_BACKEND=sqlite` | `sqlite:///./sinc.db` |
| `DEFAULT_WARNING_THRESHOLD_MINUTES` | Fallback se `monitor_settings` não estiver semeada | `5` |
| `DEFAULT_CRITICAL_THRESHOLD_MINUTES` | Fallback se `monitor_settings` não estiver semeada | `15` |
| `DEFAULT_CHECK_INTERVAL_SECONDS` | Fallback se `monitor_settings` não estiver semeada | `60` |
| `FIREBASE_CREDENTIALS_PATH` | Caminho do service account | `./firebase-adminsdk.json` |
| `FCM_TOPIC` | Tópico FCM usado como fallback de broadcast | `sinc-alerts` |
| `FCM_DRY_RUN` | Se `true`, apenas loga em vez de enviar push real | `true` |

O intervalo real entre checagens (`check_interval_seconds`) e os limites de
tempo (`warning_threshold_minutes`, `critical_threshold_minutes`) vêm da
tabela `monitor_settings` no banco (Supabase ou SQLite) — os valores no
`.env` acima são apenas um fallback usado se essa tabela ainda não tiver
sido semeada. Ver `docs/setup-supabase.md`.

## Rodando

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

A API sobe em `http://localhost:8000`. O loop de monitoramento inicia
automaticamente (`app/main.py`, evento de lifespan) e roda em background
dentro do próprio processo assíncrono do FastAPI.

Documentação interativa (Swagger): `http://localhost:8000/docs`.

## Endpoints principais

Domínio novo (unidades de sincronização):

- `GET /api/sync-units` — lista todas as unidades e status atual (send/receive/overall)
- `GET /api/sync-units/{a7_code}` — detalhe de uma unidade

Domínio legado, mantido por compatibilidade com o app Android (mapeia
`sync_units`/`sync_events` para o vocabulário `ONLINE/ATENCAO/OFFLINE` já
usado pelo app):

- `GET /api/servers` — lista todas as unidades no formato legado
- `GET /api/servers/{id}` — detalhe (`id` = `a7_code`)
- `GET /api/events?server_id=&status=&limit=` — histórico de eventos

Comuns:

- `GET /api/health` — status detalhado: `database`, `monitor`, `firebase`
- `POST /api/devices` — registra um FCM token de dispositivo
- `GET /api/devices` — lista dispositivos registrados (sem expor o token)
- `POST /api/mock/scenario` — (apenas `SOURCE_MODE=mock`) força uma
  sequência de status (`NORMAL`/`ATENCAO`/`CRITICO`) para uma unidade

Exemplo de uso do mock para simular uma unidade ficando crítica e depois
normalizando:

```bash
curl -X POST http://localhost:8000/api/mock/scenario \
  -H "Content-Type: application/json" \
  -d '{"a7_code": "A7-0001", "sequence": ["ATENCAO", "CRITICO", "CRITICO", "NORMAL"]}'
```

## Ativando a fonte real

1. Edite `.env`: `SOURCE_MODE=real`.
2. A partir de um ambiente com acesso de rede à URL real, rode:
   ```bash
   python backend/scripts/inspect_source.py
   ```
3. Ajuste `app/monitor/real_source.py` (`_parse_json` / `_parse_html`) para o
   formato exato retornado pela fonte, se necessário — o parser já procura
   por várias variações de nome de campo (`a7_code`/`codigo_a7`,
   `business_unit`/`unidade`, `last_send_at`/`ultimo_envio`, etc.).
4. Rode os testes novamente (`pytest`) para garantir que nada quebrou.

## Produção

Para produção, recomenda-se rodar atrás de um processo supervisionado
(systemd, supervisor, Docker) e usar `uvicorn` com múltiplos workers ou
`gunicorn -k uvicorn.workers.UvicornWorker`. `DB_BACKEND=supabase` é
obrigatório em produção — SQLite é apenas para desenvolvimento local e
testes automatizados.
