-- Демонстрационные данные: оргструктура, работники, информационные ресурсы.
-- Все персоналии вымышлены. Пароли задаются при запуске приложения (DemoPasswordInitializer).

INSERT INTO department (code, name, parent_id) VALUES ('10000', 'ООО «ВБ ТЕХ»', NULL);
INSERT INTO department (code, name, parent_id) VALUES
    ('11000', 'Департамент «Управление Финтех»',      (SELECT id FROM department WHERE code = '10000')),
    ('12000', 'Служба технической поддержки',          (SELECT id FROM department WHERE code = '10000')),
    ('13000', 'Отдел информационной безопасности',     (SELECT id FROM department WHERE code = '10000')),
    ('14000', 'Отдел кадрового администрирования',     (SELECT id FROM department WHERE code = '10000'));
INSERT INTO department (code, name, parent_id) VALUES
    ('11100', 'Подразделение «Платёжный шлюз»',        (SELECT id FROM department WHERE code = '11000'));

INSERT INTO employee (personnel_no, full_name, email, position, department_id, support_group_id, role) VALUES
    ('000001', 'Белов Сергей Николаевич',     'belov@vbtech.example',      'Генеральный директор',
        (SELECT id FROM department WHERE code = '10000'), NULL, 'EMPLOYEE'),
    ('000102', 'Орлов Дмитрий Павлович',      'orlov@vbtech.example',      'Директор департамента',
        (SELECT id FROM department WHERE code = '11000'), NULL, 'EMPLOYEE'),
    ('000103', 'Никитин Роман Евгеньевич',    'nikitin@vbtech.example',    'Технический руководитель',
        (SELECT id FROM department WHERE code = '11100'), NULL, 'EMPLOYEE'),
    ('000215', 'Соколова Марина Игоревна',    'sokolova@vbtech.example',   'Руководитель группы',
        (SELECT id FROM department WHERE code = '11100'), NULL, 'EMPLOYEE'),
    ('000347', 'Кравцов Илья Андреевич',      'kravtsov@vbtech.example',   'Тестировщик',
        (SELECT id FROM department WHERE code = '11100'), NULL, 'EMPLOYEE'),
    ('000352', 'Лебедева Анна Олеговна',      'lebedeva@vbtech.example',   'Разработчик серверной части',
        (SELECT id FROM department WHERE code = '11100'), NULL, 'EMPLOYEE'),
    ('000410', 'Громов Алексей Викторович',   'gromov@vbtech.example',     'Руководитель службы поддержки',
        (SELECT id FROM department WHERE code = '12000'), (SELECT id FROM support_group WHERE code = 'L1'),  'SUPPORT'),
    ('000411', 'Зайцев Павел Романович',      'zaitsev@vbtech.example',    'Специалист технической поддержки',
        (SELECT id FROM department WHERE code = '12000'), (SELECT id FROM support_group WHERE code = 'L1'),  'SUPPORT'),
    ('000412', 'Мельникова Ольга Юрьевна',    'melnikova@vbtech.example',  'Специалист по управлению доступом',
        (SELECT id FROM department WHERE code = '12000'), (SELECT id FROM support_group WHERE code = 'ACC'), 'SUPPORT'),
    ('000413', 'Тарасов Игорь Денисович',     'tarasov@vbtech.example',    'Инженер ИТ-инфраструктуры',
        (SELECT id FROM department WHERE code = '12000'), (SELECT id FROM support_group WHERE code = 'INF'), 'SUPPORT'),
    ('000501', 'Ершова Наталья Сергеевна',    'ershova@vbtech.example',    'Руководитель отдела информационной безопасности',
        (SELECT id FROM department WHERE code = '13000'), NULL, 'SECURITY'),
    ('000601', 'Фролова Елена Артуровна',     'frolova@vbtech.example',    'Специалист по кадровому администрированию',
        (SELECT id FROM department WHERE code = '14000'), NULL, 'ADMIN');

UPDATE department SET head_id = (SELECT id FROM employee WHERE personnel_no = '000001') WHERE code = '10000';
UPDATE department SET head_id = (SELECT id FROM employee WHERE personnel_no = '000102') WHERE code = '11000';
UPDATE department SET head_id = (SELECT id FROM employee WHERE personnel_no = '000215') WHERE code = '11100';
UPDATE department SET head_id = (SELECT id FROM employee WHERE personnel_no = '000410') WHERE code = '12000';
UPDATE department SET head_id = (SELECT id FROM employee WHERE personnel_no = '000501') WHERE code = '13000';
UPDATE department SET head_id = (SELECT id FROM employee WHERE personnel_no = '000601') WHERE code = '14000';

INSERT INTO info_resource (code, name, owner_id, payment_contour) VALUES
    ('R001', 'Система контроля версий',                       (SELECT id FROM employee WHERE personnel_no = '000103'), FALSE),
    ('R002', 'Стенд тестирования платёжного шлюза',           (SELECT id FROM employee WHERE personnel_no = '000103'), FALSE),
    ('R003', 'Платёжный шлюз: промышленный контур',           (SELECT id FROM employee WHERE personnel_no = '000102'), TRUE),
    ('R004', 'Система мониторинга',                           (SELECT id FROM employee WHERE personnel_no = '000413'), FALSE),
    ('R005', 'Кадровая система',                              (SELECT id FROM employee WHERE personnel_no = '000601'), FALSE);

INSERT INTO access_role (resource_id, code, name, description) VALUES
    ((SELECT id FROM info_resource WHERE code = 'R001'), 'RPRT', 'Наблюдатель',   'Чтение репозиториев и задач'),
    ((SELECT id FROM info_resource WHERE code = 'R001'), 'DEVL', 'Разработчик',   'Чтение и запись в репозитории группы'),
    ((SELECT id FROM info_resource WHERE code = 'R001'), 'MNTR', 'Сопровождающий','Управление ветками и слияниями'),
    ((SELECT id FROM info_resource WHERE code = 'R002'), 'TEST', 'Тестировщик стенда',   'Запуск тестовых сценариев, просмотр журналов'),
    ((SELECT id FROM info_resource WHERE code = 'R002'), 'ADMN', 'Администратор стенда', 'Развёртывание сборок, изменение конфигурации'),
    ((SELECT id FROM info_resource WHERE code = 'R003'), 'VIEW', 'Просмотр журналов',    'Чтение журналов операций без платёжных реквизитов'),
    ((SELECT id FROM info_resource WHERE code = 'R003'), 'OPER', 'Оператор',             'Управление маршрутами платежей'),
    ((SELECT id FROM info_resource WHERE code = 'R004'), 'VIEW', 'Просмотр',             'Просмотр панелей мониторинга'),
    ((SELECT id FROM info_resource WHERE code = 'R004'), 'EDIT', 'Редактор',             'Создание панелей и правил оповещения'),
    ((SELECT id FROM info_resource WHERE code = 'R005'), 'VIEW', 'Просмотр оргструктуры','Чтение справочника подразделений и должностей');
