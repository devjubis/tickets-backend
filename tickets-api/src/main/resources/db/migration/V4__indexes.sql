CREATE INDEX idx_tickets_status_created ON tickets (status, created_at DESC);
CREATE INDEX idx_tickets_customer       ON tickets (customer_id, created_at DESC);
CREATE INDEX idx_tickets_assigned       ON tickets (assigned_to) WHERE assigned_to IS NOT NULL;
CREATE INDEX idx_tickets_category       ON tickets (category_id);

CREATE INDEX idx_comments_ticket ON ticket_comments (ticket_id, created_at);
CREATE INDEX idx_history_ticket  ON ticket_status_history (ticket_id, changed_at DESC);

CREATE UNIQUE INDEX idx_customers_doc ON customers (document_number);
CREATE UNIQUE INDEX idx_users_email ON users (LOWER(email));

CREATE INDEX idx_users_customer ON users (customer_id) WHERE customer_id IS NOT NULL;
