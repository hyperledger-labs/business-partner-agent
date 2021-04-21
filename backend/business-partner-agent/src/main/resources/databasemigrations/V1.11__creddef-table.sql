CREATE TABLE bpa_cred_def (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    schema_id uuid, -- field name plus _id
    credential_definition_id character varying(255) UNIQUE NOT NULL,
    tag character varying(255) NOT NULL,
    is_support_revocation boolean NOT NULL,
    revocation_registry_size integer NOT NULL
);
