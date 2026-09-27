-- Нормативно-справочная информация: статусы, приоритеты, группы, виды заявок.

INSERT INTO ticket_status (code, name, is_final) VALUES
    ('AP', 'На согласовании', FALSE),
    ('QU', 'В очереди',       FALSE),
    ('WP', 'В работе',        FALSE),
    ('RS', 'Решена',          FALSE),
    ('CL', 'Закрыта',         TRUE),
    ('RJ', 'Отклонена',       TRUE);

INSERT INTO ticket_priority (code, name, sla_factor) VALUES
    (1, 'Критический', 0.25),
    (2, 'Высокий',     0.50),
    (3, 'Средний',     1.00),
    (4, 'Низкий',      1.50);

INSERT INTO support_group (code, name) VALUES
    ('L1',  'Первая линия поддержки'),
    ('ACC', 'Группа управления доступом'),
    ('INF', 'Группа ИТ-инфраструктуры');

INSERT INTO ticket_type (code, name, category, description, sla_hours, default_priority,
                         needs_approval, contains_pd, access_action, support_group_id) VALUES
    ('101', 'Неисправность рабочей станции или периферии', 'INCIDENT',
     'Не включается компьютер, не работает монитор, принтер, гарнитура и другое оборудование рабочего места',
     8, 3, FALSE, FALSE, NULL, (SELECT id FROM support_group WHERE code = 'L1')),
    ('102', 'Недоступен корпоративный сервис', 'INCIDENT',
     'Не открывается почта, мессенджер, система контроля версий, стенд или другой внутренний сервис',
     4, 2, FALSE, FALSE, NULL, (SELECT id FROM support_group WHERE code = 'INF')),
    ('103', 'Восстановление доступа: сброс пароля, разблокировка учётной записи', 'INCIDENT',
     'Учётная запись заблокирована, истёк срок действия пароля, утрачен второй фактор',
     2, 2, FALSE, TRUE, NULL, (SELECT id FROM support_group WHERE code = 'L1')),
    ('201', 'Предоставление доступа к информационной системе', 'ACCESS',
     'Выдача роли в информационной системе. Требует согласования руководителя и владельца ресурса',
     16, 3, TRUE, TRUE, 'GRANT', (SELECT id FROM support_group WHERE code = 'ACC')),
    ('202', 'Расширение состава прав в информационной системе', 'ACCESS',
     'Выдача дополнительной роли в системе, к которой доступ уже предоставлен',
     16, 3, TRUE, TRUE, 'GRANT', (SELECT id FROM support_group WHERE code = 'ACC')),
    ('203', 'Отзыв прав доступа', 'ACCESS',
     'Отзыв роли при переводе работника или изменении его обязанностей',
     8, 2, FALSE, TRUE, 'REVOKE', (SELECT id FROM support_group WHERE code = 'ACC'));
