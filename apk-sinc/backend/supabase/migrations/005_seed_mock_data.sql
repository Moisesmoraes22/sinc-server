-- Dados de TESTE (nao sao dados reais de producao).
-- Uteis para validar a API e o app Android contra um Supabase real antes
-- de a fonte real estar disponivel. Todos os codigos A7 abaixo comecam com
-- "TEST-" para deixar claro que sao ficticios e facilmente identificaveis/
-- removiveis (`delete from sync_units where a7_code like 'TEST-%';`).
--
-- Esta migration e opcional: pule-a em um ambiente de producao real
-- (basta nao aplicar 005_seed_mock_data.sql, ou apagar as linhas depois).

insert into sync_units (
    a7_code, business_unit, revisions_to_send,
    last_send_at, last_receive_at,
    send_elapsed_minutes, receive_elapsed_minutes,
    send_status, receive_status, overall_status,
    consecutive_failures, last_checked_at
) values
    ('TEST-0001', 'Unidade Matriz (dados de teste)', 0,
     now(), now(), 0, 0, 'NORMAL', 'NORMAL', 'NORMAL', 0, now()),
    ('TEST-0002', 'Unidade Filial Sul (dados de teste)', 3,
     now() - interval '8 minutes', now() - interval '2 minutes', 8, 2,
     'ATENCAO', 'NORMAL', 'ATENCAO', 1, now()),
    ('TEST-0003', 'Unidade Filial Norte (dados de teste)', 12,
     now() - interval '25 minutes', now() - interval '20 minutes', 25, 20,
     'CRITICO', 'CRITICO', 'CRITICO', 3, now())
on conflict (a7_code) do nothing;
