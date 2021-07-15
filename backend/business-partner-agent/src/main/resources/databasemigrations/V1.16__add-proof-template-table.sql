CREATE TABLE bpa_proof_template (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    name character varying(255) NOT NULL,
    attribute_groups jsonb
);

