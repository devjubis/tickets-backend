INSERT INTO roles (id, name) VALUES
    (1, 'ADMIN'),
    (2, 'AGENT'),
    (3, 'REQUESTER');

INSERT INTO ticket_categories (id, name, slug, active) VALUES
    (1, 'Suporte',        'suporte',        TRUE),
    (2, 'Visita Técnica', 'visita-tecnica', TRUE),
    (3, 'Financeiro',     'financeiro',     TRUE);
SELECT setval(pg_get_serial_sequence('ticket_categories', 'id'), 3, TRUE);
