# Testes

## Backend (executados e passando neste repositório)

```bash
cd backend
source .venv/bin/activate   # ou crie o venv conforme docs/setup-backend.md
python -m pytest -v
```

Resultado atual: **22 testes, todos passando**.

### Cobertura

`tests/test_state_machine.py` — regra de negócio central:
- 1ª falha → ATENÇÃO (sem notificar)
- 2ª falha consecutiva → ATENÇÃO (sem notificar)
- 3ª falha consecutiva → OFFLINE (notifica)
- Recuperação OFFLINE → ONLINE (notifica)
- Recuperação ATENÇÃO → ONLINE (não notifica, pois nunca chegou a OFFLINE)
- Falhas repetidas enquanto já OFFLINE não geram nova notificação (anti-spam)
- Sequência completa: 5 falhas seguidas → 1 notificação; recuperação → 1
  notificação; nova queda → 1 notificação (nunca duplicada)
- Thresholds configuráveis

`tests/test_mock_source.py` — fonte simulada:
- Todos os servidores "up" por padrão
- Cenários (`set_scenario`) consumidos em ordem
- Fallback para "up" após esgotar a sequência configurada
- Limpeza de cenários

`tests/test_real_source_parsing.py` — parser da fonte real (via `respx`,
sem depender de rede real, já que o ambiente de teste não alcança a URL real):
- Parsing de JSON em lista
- Parsing de JSON com chave `servers`/`situacao` (nomes alternativos)
- Fallback para parsing de tabela HTML
- Propagação correta de erro HTTP (5xx)

`tests/test_monitor_service.py` — integração ponta a ponta (fonte → estado →
banco → notificação):
- 3 falhas consecutivas disparam exatamente 1 notificação OFFLINE
- Recuperação dispara exatamente 1 notificação ONLINE e grava o histórico
  completo de transições (ATENÇÃO → OFFLINE → ONLINE)
- Uma falha isolada seguida de sucesso não notifica e zera o contador
- `down_count` incrementa uma vez por período de indisponibilidade (não por
  falha individual)
- Erro ao consultar a fonte não derruba o ciclo (retorna lista vazia, loga)

## Android

Testes unitários em `android/app/src/test/` (ex.: `ServerStatusTest.kt` —
parsing de status bruto vindo da API). Rodar com:

```bash
cd android
./gradlew testDebugUnitTest
```

Não foi possível executar este comando dentro do sandbox de
desenvolvimento (sem Android SDK / sem acesso aos repositórios Maven do
Google — ver `docs/build-android.md`). O código foi revisado
manualmente e uma checagem automatizada confirmou que todas as referências
`R.string.*` e `R.drawable.*` usadas no código correspondem a recursos
definidos em `res/`.

## Testando o fluxo de anti-spam manualmente (backend + mock)

Com o backend rodando em `SOURCE_MODE=mock`:

```bash
# Forca 5 falhas seguidas - so deve gerar 1 notificacao de OFFLINE
curl -X POST http://localhost:8000/api/mock/scenario \
  -H "Content-Type: application/json" \
  -d '{"server_id": "server-principal", "sequence": ["down","down","down","down","down"]}'

# aguarde os ciclos de polling (POLL_INTERVAL_SECONDS) consumirem a sequencia,
# depois confira o historico:
curl http://localhost:8000/api/events?server_id=server-principal

# forca a recuperacao - deve gerar 1 notificacao de ONLINE
curl -X POST http://localhost:8000/api/mock/scenario \
  -H "Content-Type: application/json" \
  -d '{"server_id": "server-principal", "sequence": ["up"]}'
```

Com `FCM_DRY_RUN=true` (padrão), os envios aparecem nos logs do backend
(`[FCM dry-run] ...`) em vez de serem enviados de verdade.
