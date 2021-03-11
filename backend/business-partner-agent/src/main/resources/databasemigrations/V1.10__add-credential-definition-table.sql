CREATE TABLE bpa_restrictions (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    schema_id uuid, -- field name plus _id
    label character varying(255),
    is_read_only boolean,
    issuer_did character varying(255) UNIQUE
);