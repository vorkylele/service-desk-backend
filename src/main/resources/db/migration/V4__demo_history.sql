-- Демонстрационная история: закрытые заявки за 60 дней, предшествующих 21.09.2026.
-- Дата зафиксирована, чтобы отчёты и автотесты давали одинаковый результат при любом запуске.

INSERT INTO ticket (number, type_id, status_code, priority_code, author_id, assignee_id, support_group_id,
                    subject, description, created_at, due_at, taken_at, resolved_at, closed_at,
                    resolution, sla_breached)
SELECT lpad(nextval('ticket_number_seq')::text, 8, '0'),
       tt.id, 'CL', tt.default_priority, au.id, ex.id, tt.support_group_id,
       tt.name, 'Обращение из демонстрационной истории', g.created_at,
       g.created_at + (tt.sla_hours * 3 || ' hours')::interval,
       g.created_at + interval '20 minutes',
       g.created_at + (tt.sla_hours * 3 * g.load_factor || ' hours')::interval,
       g.created_at + (tt.sla_hours * 3 * g.load_factor || ' hours')::interval + interval '1 hour',
       'Выполнено',
       g.load_factor > 1.0
FROM (SELECT n,
             TIMESTAMP '2026-09-21 00:00:00' - ((n % 60) + 1) * interval '1 day' + interval '10 hours'
                 + (n % 7) * interval '37 minutes'                                   AS created_at,
             0.12 + ((n * 37) % 100) / 100.0 * 1.02                                  AS load_factor,  -- доля норматива
             (n * 7) % 100                                                           AS dice
      FROM generate_series(1, 320) AS n) g
JOIN ticket_type tt ON tt.code = CASE
        WHEN g.dice < 30 THEN '101' WHEN g.dice < 46 THEN '102' WHEN g.dice < 63 THEN '103'
        WHEN g.dice < 85 THEN '201' WHEN g.dice < 94 THEN '202' ELSE '203' END
JOIN LATERAL (SELECT id FROM employee WHERE role = 'EMPLOYEE'
              ORDER BY id OFFSET (g.n % 6) LIMIT 1) au ON TRUE
JOIN LATERAL (SELECT id FROM employee WHERE support_group_id = tt.support_group_id
              ORDER BY id OFFSET (g.n % GREATEST((SELECT count(*) FROM employee e2
                                                  WHERE e2.support_group_id = tt.support_group_id), 1)) LIMIT 1) ex ON TRUE
WHERE extract(isodow FROM g.created_at) < 6;
