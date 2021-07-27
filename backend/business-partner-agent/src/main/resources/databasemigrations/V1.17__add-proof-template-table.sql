CREATE TABLE bpa_proof_template (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone NOT NULL,
    name character varying(255) NOT NULL,
    attribute_groups jsonb
);

ALTER TABLE partner_proof ADD COLUMN proof_template_id uuid; -- field name plus _id

ALTER TABLE partner_proof
    ADD CONSTRAINT partner_proof_template_fk_1
        FOREIGN KEY (proof_template_id)
            REFERENCES bpa_proof_template(id);
