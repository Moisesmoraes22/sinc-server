-- Indices para as consultas mais frequentes do backend e da API.

create unique index if not exists idx_sync_units_a7_code on sync_units (a7_code);
create index if not exists idx_sync_units_business_unit on sync_units (business_unit);
create index if not exists idx_sync_units_overall_status on sync_units (overall_status);
create index if not exists idx_sync_units_last_checked_at on sync_units (last_checked_at desc);

create index if not exists idx_sync_events_unit_id on sync_events (unit_id);
create index if not exists idx_sync_events_created_at on sync_events (created_at desc);
create index if not exists idx_sync_events_a7_code on sync_events (a7_code);
create index if not exists idx_sync_events_new_status on sync_events (new_status);

create unique index if not exists idx_devices_fcm_token on devices (fcm_token);
create index if not exists idx_devices_active on devices (active);
