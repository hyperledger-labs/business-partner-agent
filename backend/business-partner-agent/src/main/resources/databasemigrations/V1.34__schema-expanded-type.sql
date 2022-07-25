ALTER TABLE bpa_schema ADD COLUMN expanded_type character varying(255);

ALTER TABLE partner_proof
    ADD CONSTRAINT partner_to_proof_fk_1
        FOREIGN KEY (partner_id)
            REFERENCES partner(id);