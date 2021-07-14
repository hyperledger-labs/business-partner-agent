CREATE TABLE bpa_proof_template (
    id uuid PRIMARY KEY,
    name character varying(255) NOT NULL,
    attribute_groups jsonb
);

