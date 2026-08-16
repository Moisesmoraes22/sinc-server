-- Seed das configuracoes iniciais do monitor (linha singleton).
-- Valores conforme especificacao do produto:
--   warning_threshold_minutes = 5
--   critical_threshold_minutes = 15
--   check_interval_seconds = 60

insert into monitor_settings (id, warning_threshold_minutes, critical_threshold_minutes, check_interval_seconds)
values (1, 5, 15, 60)
on conflict (id) do nothing;
