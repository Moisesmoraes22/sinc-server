-- OMG SINC - reset de dominio: de "monitoramento de sincronizacao de lojas"
-- para "app pessoal de saude/beleza/bem-estar/habitos".
--
-- Este e um pivot de produto, nao uma evolucao incremental do schema
-- anterior: sync_units/sync_events/monitor_settings nao tem equivalente no
-- novo dominio e sao removidas. `devices` (registro de push FCM) e a unica
-- tabela que sobrevive sem alteracoes - lembretes de habito usam a mesma
-- infraestrutura de notificacao que os alertas de loja usavam.
--
-- ATENCAO: rodar esta migration contra um projeto Supabase que ainda tem
-- dados de producao do dominio antigo APAGA esses dados permanentemente
-- (drop table). So aplicar depois de confirmar que os dados de
-- sync_units/sync_events podem ser descartados.

drop trigger if exists trg_sync_units_updated_at on sync_units;
drop trigger if exists trg_monitor_settings_updated_at on monitor_settings;

drop table if exists sync_events;
drop table if exists sync_units;
drop table if exists monitor_settings;

-- ---------------------------------------------------------------------
-- profiles: perfil do usuario do app. Singular por instalacao por enquanto
-- (sem autenticacao/multi-usuario ainda) - o backend sempre usa a primeira
-- linha encontrada (get_or_create_profile), mesmo padrao que
-- monitor_settings tinha como singleton.
-- ---------------------------------------------------------------------
create table if not exists profiles (
    id uuid primary key default gen_random_uuid(),
    display_name text not null default 'Voce',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

comment on table profiles is 'Perfil do usuario do OMG SINC. Uma linha por instalacao ate haver autenticacao real.';

-- ---------------------------------------------------------------------
-- habits: habitos configurados pelo usuario (agua, sono, skincare, etc.)
-- ---------------------------------------------------------------------
create table if not exists habits (
    id uuid primary key default gen_random_uuid(),
    profile_id uuid not null references profiles (id) on delete cascade,
    title text not null,
    category text not null,
    icon_key text not null default 'circle',
    target_value numeric,
    target_unit text,
    color_tag text not null default 'ACCENT',
    active boolean not null default true,
    sort_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint habits_category_check check (category in ('AGUA', 'SONO', 'EXERCICIO', 'SKINCARE', 'HUMOR', 'OUTRO')),
    constraint habits_color_tag_check check (color_tag in ('ACCENT', 'SUCCESS', 'WARNING'))
);

comment on table habits is 'Habitos que o usuario acompanha diariamente.';

-- ---------------------------------------------------------------------
-- habit_logs: registro diario de progresso por habito (1 linha por dia)
-- ---------------------------------------------------------------------
create table if not exists habit_logs (
    id bigint generated always as identity primary key,
    habit_id uuid not null references habits (id) on delete cascade,
    log_date date not null,
    value numeric,
    completed boolean not null default false,
    logged_at timestamptz not null default now(),

    constraint habit_logs_habit_date_key unique (habit_id, log_date)
);

comment on table habit_logs is 'Progresso diario de um habito. Upsert por (habit_id, log_date) - um registro por dia.';

-- ---------------------------------------------------------------------
-- metrics: serie historica de metricas de saude/bem-estar registradas
-- pelo usuario (sono, agua, humor, passos, peso)
-- ---------------------------------------------------------------------
create table if not exists metrics (
    id bigint generated always as identity primary key,
    profile_id uuid not null references profiles (id) on delete cascade,
    metric_type text not null,
    value numeric not null,
    recorded_at timestamptz not null default now(),

    constraint metrics_type_check check (
        metric_type in ('SLEEP_HOURS', 'WATER_ML', 'MOOD_SCORE', 'STEPS', 'WEIGHT_KG')
    )
);

comment on table metrics is 'Leituras de metricas de saude/bem-estar ao longo do tempo, registradas pelo usuario.';

-- ---------------------------------------------------------------------
-- indices
-- ---------------------------------------------------------------------
create index if not exists idx_habits_profile_id on habits (profile_id);
create index if not exists idx_habits_active on habits (active);

create index if not exists idx_habit_logs_habit_id on habit_logs (habit_id);
create index if not exists idx_habit_logs_log_date on habit_logs (log_date desc);

create index if not exists idx_metrics_profile_id on metrics (profile_id);
create index if not exists idx_metrics_type on metrics (metric_type);
create index if not exists idx_metrics_recorded_at on metrics (recorded_at desc);

-- ---------------------------------------------------------------------
-- updated_at automatico (reaproveita a funcao ja hardenizada em 006)
-- ---------------------------------------------------------------------
drop trigger if exists trg_profiles_updated_at on profiles;
create trigger trg_profiles_updated_at
    before update on profiles
    for each row execute function set_updated_at();

drop trigger if exists trg_habits_updated_at on habits;
create trigger trg_habits_updated_at
    before update on habits
    for each row execute function set_updated_at();

-- ---------------------------------------------------------------------
-- RLS - mesmo padrao das tabelas anteriores: habilitado, sem policies
-- permissivas. Somente a SERVICE_ROLE_KEY (usada exclusivamente pelo
-- backend) tem acesso; anon/authenticated ficam bloqueados por padrao.
-- ---------------------------------------------------------------------
alter table profiles enable row level security;
alter table habits enable row level security;
alter table habit_logs enable row level security;
alter table metrics enable row level security;
