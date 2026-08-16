# Testes

## Backend (executados e passando neste repositório)

```bash
cd backend
source .venv/bin/activate   # ou crie o venv conforme docs/setup-backend.md
python -m pytest -v
```

Resultado atual: **37 testes, todos passando**. Nenhum teste depende de um
Supabase real — todos usam `SqliteRepository` (banco SQLite em memória),
injetado via `tests/conftest.py`.

### Cobertura

`tests/test_state_machine.py` — regra de status por tempo decorrido:
- Abaixo do limite de atenção → NORMAL
- No limite de atenção (ou acima, até o limite crítico) → ATENÇÃO
- No limite crítico (ou acima) → CRÍTICO
- Sem nenhum envio/recebimento registrado → CRÍTICO
- `overall_status` é sempre o pior entre `send_status` e `receive_status`
- Anti-spam: `should_notify` só é `True` quando o status muda de fato
  (`NORMAL→ATENCAO`, `ATENCAO→CRITICO`, `CRITICO→NORMAL`); repetir o mesmo
  status nunca notifica

`tests/test_mock_source.py` — fonte simulada:
- Unidades padrão geradas como NORMAL
- Cenários (`set_scenario`) com rótulos NORMAL/ATENCAO/CRITICO consumidos em
  ordem, traduzidos em tempo decorrido coerente com os thresholds padrão
- Fallback para NORMAL após esgotar a sequência configurada
- Limpeza de cenários

`tests/test_real_source_parsing.py` — parser da fonte real (via `respx`,
sem depender de rede real):
- Parsing de JSON em lista, com nomes de campo em inglês
- Parsing de JSON com nomes de campo em português (`codigo_a7`, `unidade`,
  `ultimo_envio`, `ultimo_recebimento`) e payload envelopado em `data`
- Fallback para parsing de tabela HTML (casando colunas pelo cabeçalho)
- Propagação correta de erro HTTP (5xx)

`tests/test_repository.py` — `SqliteRepository` (contrato usado também
pelo `SupabaseRepository`, ver `app/database/base.py`):
- Criação e atualização (upsert) de unidade por `a7_code`
- Unidade inexistente retorna `None`
- Listagem ordenada
- Registro e listagem de eventos, com filtro por status
- Busca do último evento de uma unidade
- Registro de dispositivo é idempotente (mesmo token não duplica)
- Listagem de tokens ativos
- Configurações (`monitor_settings`): valores padrão e atualização
- `health_check`

`tests/test_monitor_service.py` — integração ponta a ponta (fonte → estado
→ repository → evento → notificação):
- Transição para CRÍTICO gera exatamente 1 notificação e 1 evento
  `PROBLEM_STARTED`
- Repetir CRÍTICO em ciclos seguintes não gera nova notificação nem novo
  evento (anti-spam)
- Recuperação (CRÍTICO → NORMAL) notifica e grava `PROBLEM_RESOLVED` com
  `duration_seconds` calculado a partir do evento anterior
- Transição para ATENÇÃO notifica sem chegar a CRÍTICO
- `send_status`/`receive_status` avaliados de forma independente
- Erro ao consultar a fonte não derruba o ciclo (retorna lista vazia, loga)

`tests/test_api.py` — endpoints FastAPI (via `TestClient`, com o
repository de teste injetado por `dependency_overrides`):
- `GET /api/health` reporta `database: connected`
- `GET /api/sync-units` e `GET /api/servers` (formato legado) refletem os
  mesmos dados, com o status traduzido para o vocabulário legado
  (`CRITICO → OFFLINE`)
- `GET /api/servers/{id}` inexistente retorna 404
- `POST /api/devices` + `GET /api/devices` (sem expor o token na resposta)
- `GET /api/events` traduz `from_status`/`to_status` para o vocabulário legado

## Android

Testes unitários em `android/app/src/test/` (ex.: `ServerStatusTest.kt` —
parsing de status bruto vindo da API). Rodar com:

```bash
cd android
./gradlew testDebugUnitTest
```

Não foi possível executar este comando dentro do sandbox de
desenvolvimento (sem Android SDK / sem acesso aos repositórios Maven do
Google — ver `docs/build-android.md`). O contrato JSON consumido pelo app
(`GET /api/servers`, `GET /api/events`, `GET /api/devices`) não mudou com a
migração para Supabase, então o app não precisou de nenhuma alteração.

## Testando o fluxo de anti-spam manualmente (backend + mock)

Com o backend rodando em `SOURCE_MODE=mock`:

```bash
# Forca a unidade a ficar critica por varios ciclos - so deve gerar 1 notificacao
curl -X POST http://localhost:8000/api/mock/scenario \
  -H "Content-Type: application/json" \
  -d '{"a7_code": "A7-0001", "sequence": ["CRITICO","CRITICO","CRITICO","CRITICO"]}'

# aguarde os ciclos de polling consumirem a sequencia, depois confira o historico:
curl http://localhost:8000/api/sync-units/A7-0001
curl http://localhost:8000/api/events?server_id=A7-0001

# forca a recuperacao - deve gerar 1 notificacao de normalizacao
curl -X POST http://localhost:8000/api/mock/scenario \
  -H "Content-Type: application/json" \
  -d '{"a7_code": "A7-0001", "sequence": ["NORMAL"]}'
```

Com `FCM_DRY_RUN=true` (padrão), os envios aparecem nos logs do backend
(`[FCM dry-run] ...`) em vez de serem enviados de verdade.
