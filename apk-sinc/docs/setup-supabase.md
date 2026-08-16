# Setup do Supabase

O Supabase (PostgreSQL) é o banco principal do backend: armazena as
unidades monitoradas (`sync_units`), o histórico de eventos
(`sync_events`), os dispositivos Android registrados para push
(`devices`) e as configurações do monitor (`monitor_settings`). O backend
Python/FastAPI continua sendo quem monitora a fonte real e aplica a
lógica de negócio — o Supabase é só o armazenamento.

> **Status atual deste projeto:** já existe um projeto Supabase real criado
> e com as migrations aplicadas — `apk-sinc` (ref `ajiavjvynpiqlrggzypk`,
> região `sa-east-1`, URL `https://ajiavjvynpiqlrggzypk.supabase.co`).
> As 4 tabelas, índices e RLS foram validados (ver seção 9). Se você é o
> dono desse projeto, pule direto para a seção 4 (configurar `.env`) — só
> falta colar a `SERVICE_ROLE_KEY` (Project Settings → API Keys). Se
> precisar de um projeto novo (ex.: ambiente separado), siga o passo a
> passo abaixo normalmente.

## 1. Criar o projeto no Supabase

1. Acesse https://supabase.com/dashboard e crie uma conta/organização, se
   ainda não tiver.
2. Clique em **New Project**, escolha um nome (ex.: `apk-sinc`), uma senha
   de banco de dados forte (guarde-a) e a região mais próxima.
3. Aguarde o provisionamento (leva alguns minutos).

## 2. Onde encontrar a URL e a SERVICE_ROLE_KEY

No painel do projeto:

1. **Project Settings → Data API** (ou **Settings → API**, dependendo da
   versão do painel):
   - **Project URL** → copie para `SUPABASE_URL`.
2. **Project Settings → API Keys** (chamada de **API** em versões mais
   antigas do painel):
   - **service_role** (chave secreta, com privilégios administrativos) →
     copie para `SUPABASE_SERVICE_ROLE_KEY`.
   - **NUNCA** use a chave `anon`/`public` no backend administrativo, e
     **NUNCA** copie a `service_role` para o app Android — ela dá acesso
     total ao banco, ignorando RLS.

## 3. Executar as migrations

As migrations ficam em `backend/supabase/migrations/`, numeradas em ordem
de aplicação:

```
001_initial_schema.sql   -- tabelas: sync_units, sync_events, devices, monitor_settings
002_indexes.sql          -- indices nas colunas mais consultadas
003_rls.sql              -- habilita Row Level Security (sem policies publicas)
004_seed_settings.sql    -- semeia a linha unica de monitor_settings (5 / 15 / 60)
005_seed_mock_data.sql   -- (opcional) unidades de TESTE, claramente marcadas "TEST-"
```

### Opção A — Supabase CLI (recomendado)

```bash
npm install -g supabase
supabase login
supabase link --project-ref SEU_PROJECT_REF   # veja o ref na URL do painel
supabase db push                               # aplica as migrations em backend/supabase/migrations/
```

Rode o comando a partir da pasta `backend/`, para que o CLI encontre a
pasta `supabase/migrations/` automaticamente.

### Opção B — SQL Editor do painel (manual)

1. Abra **SQL Editor** no painel do Supabase.
2. Cole e execute o conteúdo de `001_initial_schema.sql`, depois
   `002_indexes.sql`, depois `003_rls.sql`, depois `004_seed_settings.sql`
   — nessa ordem.
3. Execute `005_seed_mock_data.sql` apenas se quiser dados de teste para
   validar a API/o app antes de a fonte real estar disponível.

## 4. Configurar o `.env` do backend

```bash
cd backend
cp .env.example .env
```

Edite `.env`:

```
DB_BACKEND=supabase
SUPABASE_URL=https://SEU-PROJETO.supabase.co
SUPABASE_SERVICE_ROLE_KEY=coloque-a-chave-service_role-aqui
```

A `SUPABASE_SERVICE_ROLE_KEY` **fica somente neste arquivo `.env`**, que
está no `.gitignore` e nunca deve ser commitado. O app Android nunca lê
esse valor — ele fala apenas com a API FastAPI (ver `docs/architecture.md`).

## 5. Iniciar o backend

```bash
pip install -r requirements.txt
uvicorn app.main:app --reload
```

Se `SUPABASE_URL`/`SUPABASE_SERVICE_ROLE_KEY` estiverem ausentes ou
incorretos com `DB_BACKEND=supabase`, o backend falha ao iniciar com uma
mensagem clara (`SupabaseNotConfiguredError`) apontando para este
documento.

## 6. Verificar a conexão

```bash
curl http://localhost:8000/api/health
```

Resposta esperada com tudo certo:

```json
{
  "status": "ok",
  "database": "connected",
  "monitor": "running",
  "firebase": "dry-run",
  "source_mode": "mock",
  "db_backend": "supabase",
  "last_poll_at": "2026-...",
  "units_count": 0
}
```

`"database": "error"` indica falha de conexão/credencial — confira URL e
chave. `"units_count"` deve virar > 0 depois do primeiro ciclo de
monitoramento (ou imediatamente, se você rodou `005_seed_mock_data.sql`).

## 7. Testar o banco diretamente

```bash
curl http://localhost:8000/api/sync-units
curl http://localhost:8000/api/servers      # formato legado, usado pelo app Android
```

Ou, direto no Supabase (SQL Editor):

```sql
select a7_code, business_unit, overall_status, last_checked_at from sync_units;
select * from sync_events order by created_at desc limit 20;
select device_name, active, last_seen_at from devices;
select * from monitor_settings;
```

## 8. Alternativa sem Supabase (desenvolvimento local rápido)

Para desenvolver sem configurar um projeto Supabase, use SQLite local:

```
DB_BACKEND=sqlite
SQLITE_DATABASE_URL=sqlite:///./sinc.db
```

O schema é criado automaticamente na primeira execução (mesmas tabelas,
via SQLAlchemy — ver `app/models/orm.py`). **Isso é só para desenvolvimento
local**: o ambiente de produção deve sempre usar `DB_BACKEND=supabase`.
Os testes automatizados (`pytest`) sempre usam SQLite em memória
diretamente, independente deste flag — nunca dependem de um Supabase real.

## Políticas de Row Level Security (RLS)

Todas as quatro tabelas têm RLS **habilitado** (`003_rls.sql`), mas
**nenhuma policy permissiva é criada** para os papéis `anon`/`authenticated`
— ou seja, qualquer acesso que não seja com a `service_role` é negado por
padrão. O backend usa a `service_role`, que ignora RLS (`bypassrls`) e
continua funcionando normalmente. Isso garante que, mesmo que a chave
`anon`/`public` do projeto vaze ou seja usada por engano em algum cliente,
nenhuma tabela fica exposta.

Se no futuro for necessário expor leitura pública (ex.: um dashboard
somente-leitura sem autenticação), crie uma policy explícita e restrita —
um exemplo comentado está em `003_rls.sql`.

## 9. Status da validação contra o projeto real (`apk-sinc`)

As migrations 001–006 foram aplicadas com sucesso no projeto real via MCP
do Supabase (`apply_migration`), e o schema foi validado diretamente no
banco (não só lido do código):

- **Tabelas:** `sync_units` (3 linhas de teste `TEST-0001/2/3`),
  `sync_events` (0), `devices` (0), `monitor_settings` (1, com os valores
  padrão 5/15/60) — confirmadas via `list_tables`.
- **Índices:** os 13 índices esperados (a7_code único, business_unit,
  overall_status, last_checked_at, unit_id, created_at, a7_code em
  sync_events, new_status, fcm_token único, active) — confirmados via
  `pg_indexes`.
- **RLS:** habilitado nas 4 tabelas, sem policies para `anon`/`authenticated`
  — confirmado via `get_advisors` (só aparecem avisos `INFO` esperados de
  "RLS habilitado sem policy", que é o comportamento desejado).
- **Constraints:** os `check` de status (`NORMAL`/`ATENCAO`/`CRITICO`) e as
  chaves únicas (`a7_code`, `fcm_token`) foram exercitados com sucesso ao
  simular via SQL o ciclo completo `NORMAL → ATENÇÃO → ATENÇÃO → CRÍTICO →
  CRÍTICO → NORMAL`: exatamente 3 eventos foram gravados (não 6), provando
  que o anti-spam também é coerente no nível de dados. Os dados dessa
  simulação foram removidos depois de validados.
- Foi corrigido um aviso de segurança do linter do Supabase
  (`function_search_path_mutable`) na função de trigger `set_updated_at`
  — ver `006_harden_function_search_path.sql`.

### Duas pegadinhas encontradas e corrigidas ao conectar num projeto real

1. **Formato novo de API key (`sb_secret_...`) não é aceito por
   `supabase-py` antigo.** Projetos Supabase criados recentemente usam o
   novo formato de chave (`sb_publishable_...` / `sb_secret_...`, sem
   pontos), mas `supabase-py==2.9.1` (a versão originalmente fixada em
   `requirements.txt`) valida a key com uma regex que exige formato JWT
   (com pontos) e falha com `SupabaseException: Invalid API key`. Corrigido
   fixando `supabase==2.31.0` em `requirements.txt`, com um teste de
   regressão em `tests/test_supabase_client.py` garantindo que o formato
   novo é aceito. **Se você usa uma `service_role key` no formato antigo
   (um JWT longo começando com `eyJ...`), qualquer versão recente também
   funciona normalmente.**
2. **Ambientes de desenvolvimento com egress restrito podem bloquear
   `*.supabase.co`.** O sandbox usado para construir/validar este projeto
   tem uma política de rede que só libera HTTPS para uma lista curta de
   hosts (registry.npmjs.org, pypi.org, etc.) — `*.supabase.co` **não**
   está nela, então o processo do backend (`uvicorn`) não conseguiu
   completar chamadas REST reais ao Supabase a partir de lá (erro
   `403 Forbidden` do proxy de saída). Isso **não é um problema do código**:
   os testes de schema/CRUD/anti-spam acima foram feitos via SQL direto no
   projeto real (usando a integração MCP do Supabase, que não passa por
   esse proxy), e a suíte de testes automatizados roda 100% contra SQLite,
   sem depender de rede. **Rode o backend numa máquina com acesso normal à
   internet** (a sua, um servidor, uma VPS) para ver
   `GET /api/health` retornar `"database": "connected"` de verdade — não há
   nenhuma razão para isso falhar fora de um ambiente com egress
   restrito como este.
