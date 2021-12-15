
CREATE TABLE bpa_transaction (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    partner_id uuid NOT NULL,
    role character varying(255) NOT NULL,
    thread_id character varying(255),
    transaction_id character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    type character varying(255),
    endorser_write_transaction boolean NOT NULL,
    expires_at timestamp without time zone,
    updated_at timestamp without time zone
);
