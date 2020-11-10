CREATE TABLE bpawebhook (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    webhook jsonb
);
CREATE UNIQUE INDEX unique_webhook_url_idx ON bpawebhook( (webhook->>'url') ) ;