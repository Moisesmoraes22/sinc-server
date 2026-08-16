-- Row Level Security
--
-- O backend acessa o Supabase usando a SERVICE_ROLE_KEY, que por padrao
-- ignora RLS (bypassrls). As politicas abaixo existem para garantir que,
-- caso alguma credencial diferente (anon/authenticated) seja usada por
-- engano ou no futuro (ex.: acesso direto de um app), nenhuma tabela fique
-- exposta publicamente por omissao.
--
-- Nenhuma politica permissiva e criada para "anon" ou "authenticated":
-- isso significa que, com qualquer chave que nao seja a service role,
-- todas as operacoes (select/insert/update/delete) sao negadas por padrao.
-- O aplicativo Android NUNCA deve receber a SERVICE_ROLE_KEY - ele fala
-- somente com a API FastAPI, que e quem detem essa chave.

alter table sync_units enable row level security;
alter table sync_events enable row level security;
alter table devices enable row level security;
alter table monitor_settings enable row level security;

-- Nenhuma policy e criada intencionalmente: sem policies, o acesso via
-- anon/authenticated fica bloqueado por padrao (deny-by-default do
-- Postgres/Supabase quando RLS esta habilitado). Se no futuro for
-- necessario expor leitura publica limitada (ex.: um dashboard publico
-- somente-leitura), criar uma policy explicita e restrita aqui, por
-- exemplo:
--
-- create policy "leitura publica de status agregado"
--     on sync_units for select
--     to anon
--     using (true);
--
-- Por ora, mantemos o acesso fechado: apenas o backend (service role)
-- le e escreve nessas tabelas.
