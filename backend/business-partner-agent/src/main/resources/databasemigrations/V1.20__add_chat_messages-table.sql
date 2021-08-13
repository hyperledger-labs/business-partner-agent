CREATE TABLE chat_message (
    id uuid PRIMARY KEY,
    partner_id uuid NOT NULL,
    content text,
    incoming boolean NOT NULL,
    created_at timestamp without time zone
);
