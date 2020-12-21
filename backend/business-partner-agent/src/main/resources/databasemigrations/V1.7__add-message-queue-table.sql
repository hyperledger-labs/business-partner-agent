CREATE TABLE message_queue (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    message jsonb
);