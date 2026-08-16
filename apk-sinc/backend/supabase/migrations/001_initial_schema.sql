-- APK SINC - schema inicial
-- Cria as tabelas principais usadas pelo backend como fonte de verdade:
-- unidades monitoradas (sync_units), historico de eventos (sync_events),
-- dispositivos Android registrados para push (devices) e configuracoes
-- do monitor (monitor_settings).

create extension if not exists "pgcrypto";

-- ---------------------------------------------------------------------
-- sync_units: unidades de negocio monitoradas (identificadas por codigo A7)
-- ---------------------------------------------------------------------
create table if not exists sync_units (
    id uuid primary key default gen_random_uuid(),
    a7_code text not null,
    business_unit text not null,
    revisions_to_send integer,
    last_send_at timestamptz,
    last_receive_at timestamptz,
    send_elapsed_minutes numeric,
    receive_elapsed_minutes numeric,
    send_status text not null default 'NORMAL',
    receive_status text not null default 'NORMAL',
    overall_status text not null default 'NORMAL',
    consecutive_failures integer not null default 0,
    last_checked_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint sync_units_a7_code_key unique (a7_code),
    constraint sync_units_send_status_check check (send_status in ('NORMAL', 'ATENCAO', 'CRITICO')),
    constraint sync_units_receive_status_check check (receive_status in ('NORMAL', 'ATENCAO', 'CRITICO')),
    constraint sync_units_overall_status_check check (overall_status in ('NORMAL', 'ATENCAO', 'CRITICO'))
);

comment on table sync_units is 'Unidades de negocio monitoradas pela sincronizacao (identificadas pelo codigo A7).';
comment on column sync_units.a7_code is 'Codigo A7 - identificador estavel e unico da unidade na fonte de monitoramento.';

-- ---------------------------------------------------------------------
-- sync_events: historico de mudancas de status por unidade
-- ---------------------------------------------------------------------
create table if not exists sync_events (
    id bigint generated always as identity primary key,
    unit_id uuid references sync_units (id) on delete set null,
    a7_code text not null,
    business_unit text not null,
    event_type text not null,
    direction text not null,
    previous_status text,
    new_status text not null,
    started_at timestamptz,
    resolved_at timestamptz,
    duration_seconds integer,
    message text,
    created_at timestamptz not null default now(),

    constraint sync_events_event_type_check check (event_type in ('STATUS_CHANGE', 'PROBLEM_STARTED', 'PROBLEM_RESOLVED')),
    constraint sync_events_direction_check check (direction in ('SEND', 'RECEIVE', 'BOTH')),
    constraint sync_events_new_status_check check (new_status in ('NORMAL', 'ATENCAO', 'CRITICO'))
);

comment on table sync_events is 'Historico de transicoes de status das unidades monitoradas.';

-- ---------------------------------------------------------------------
-- devices: dispositivos Android registrados para notificacao push (FCM)
-- ---------------------------------------------------------------------
create table if not exists devices (
    id uuid primary key default gen_random_uuid(),
    device_name text,
    fcm_token text not null,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    last_seen_at timestamptz,

    constraint devices_fcm_token_key unique (fcm_token)
);

comment on table devices is 'Dispositivos Android registrados para receber notificacoes push via FCM.';
comment on column devices.fcm_token is 'Token FCM do dispositivo. Nunca exposto publicamente - apenas o backend le/escreve esta tabela via service role.';

-- ---------------------------------------------------------------------
-- monitor_settings: configuracoes globais do monitor (linha unica)
-- ---------------------------------------------------------------------
create table if not exists monitor_settings (
    id smallint primary key default 1,
    warning_threshold_minutes integer not null default 5,
    critical_threshold_minutes integer not null default 15,
    check_interval_seconds integer not null default 60,
    updated_at timestamptz not null default now(),

    constraint monitor_settings_singleton check (id = 1)
);

comment on table monitor_settings is 'Configuracoes globais do monitor. Tabela singleton (sempre id=1).';

-- updated_at automatico
-- search_path fixado explicitamente (recomendacao do linter de seguranca do
-- Supabase: function_search_path_mutable) para evitar sequestro via search_path.
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

drop trigger if exists trg_sync_units_updated_at on sync_units;
create trigger trg_sync_units_updated_at
    before update on sync_units
    for each row execute function set_updated_at();

drop trigger if exists trg_devices_updated_at on devices;
create trigger trg_devices_updated_at
    before update on devices
    for each row execute function set_updated_at();

drop trigger if exists trg_monitor_settings_updated_at on monitor_settings;
create trigger trg_monitor_settings_updated_at
    before update on monitor_settings
    for each row execute function set_updated_at();
