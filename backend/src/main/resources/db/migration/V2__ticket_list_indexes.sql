CREATE INDEX idx_tickets_created_at_id ON tickets (created_at, id);
CREATE INDEX idx_tickets_status_created_at_id ON tickets (status, created_at, id);
