CREATE TABLE active_rules (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    trigger jsonb,
    action jsonb
);