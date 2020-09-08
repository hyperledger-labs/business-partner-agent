CREATE TABLE public."schema" (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    label character varying(255),
    type character varying(255) UNIQUE,
    schema_id character varying(255) UNIQUE,
    seq_no bigint NOT NULL
);