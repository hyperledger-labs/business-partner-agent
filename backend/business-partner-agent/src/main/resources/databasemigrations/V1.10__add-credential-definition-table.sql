CREATE TABLE bpacredential_definition (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    schema_id uuid,
    label character varying(255),
    is_read_only boolean,
    credential_definition_id character varying(255) UNIQUE
);

CREATE TABLE entity_one (
    id uuid PRIMARY KEY,
    name character varying(255)
);

CREATE TABLE entity_many (
    id uuid PRIMARY KEY,
    -- field name plus _id
    one_id uuid,
    counter character varying(255)
);