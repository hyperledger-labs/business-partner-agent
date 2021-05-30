CREATE TABLE did_doc_web (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    did_doc jsonb,
    profile_json jsonb
);

CREATE TABLE my_credential (
    id uuid PRIMARY KEY,
    issued_at timestamp without time zone,
    type character varying(255),
    is_public boolean NOT NULL,
    referent character varying(255),
    connection_id character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    thread_id character varying(255) NOT NULL,
    label character varying(255),
    credential jsonb
);

CREATE TABLE my_document (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    type character varying(255) NOT NULL,
    is_public boolean NOT NULL,
    label character varying(255),
    document jsonb
);

CREATE TABLE partner (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    did character varying(255) NOT NULL,
    aries_support boolean NOT NULL,
    connection_id character varying(255),
    state character varying(255),
    label character varying(255),
    alias character varying(255),
    incoming boolean,
    valid boolean,
    verifiable_presentation jsonb
);

CREATE TABLE partner_proof (
    id uuid PRIMARY KEY,
    partner_id uuid NOT NULL,
    created_at timestamp without time zone NOT NULL,
    issued_at timestamp without time zone,
    type character varying(255),    
    valid boolean,
    state character varying(255),
    presentation_exchange_id character varying(255) NOT NULL,
    issuer character varying(255),
    schema_id character varying(255),
    role character varying(255),
    proof jsonb
);

CREATE TABLE bpauser (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    username character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    roles character varying(255) NOT NULL
);

CREATE TABLE bpastate (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    web_only boolean
);

CREATE TABLE bpaschema (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    label character varying(255),
    type character varying(255) UNIQUE,
    schema_id character varying(255) UNIQUE,
    seq_no bigint NOT NULL
);