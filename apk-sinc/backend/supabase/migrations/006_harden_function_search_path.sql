-- Corrige aviso do linter de seguranca do Supabase (function_search_path_mutable):
-- fixa o search_path da funcao de trigger para evitar sequestro via search_path.
-- Necessaria apenas para bancos que ja aplicaram 001_initial_schema.sql ANTES
-- desta correcao ter sido incorporada nela. Em uma instalacao nova, o
-- 001_initial_schema.sql atual ja cria a funcao correta e esta migration e
-- redundante (mas inofensiva, graças ao `create or replace`).
create or replace function set_updated_at()
returns trigger
language plpgsql
set search_path = public, pg_temp
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;
