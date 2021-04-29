CREATE TABLE bpa_credential_exchange (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    schema_id uuid NOT NULL, -- schema table id (-> schema id)
    cred_def_id uuid NOT NULL, -- cred_def table id (-> credential definition id)
    partner_id uuid NOT NULL, -- partner table id (-> connection id)
    type character varying(255) NOT NULL,
    label character varying(255),
    thread_id character varying(255) NOT NULL,
    credential_exchange_id character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    credential_offer jsonb,
    credential_proposal jsonb,
    credential jsonb,
    updated_at timestamp without time zone
);

