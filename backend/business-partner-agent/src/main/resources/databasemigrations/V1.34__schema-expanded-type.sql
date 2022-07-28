ALTER TABLE bpa_schema ADD COLUMN expanded_type character varying(255);
ALTER TABLE partner_proof ADD COLUMN credential_exchange_id uuid;

ALTER TABLE partner_proof
    ADD CONSTRAINT partner_to_proof_fk_1
        FOREIGN KEY (partner_id)
            REFERENCES partner(id);

ALTER TABLE partner_proof
    ADD CONSTRAINT bpa_credential_exchange_to_proof_fk_1
        FOREIGN KEY (credential_exchange_id)
            REFERENCES bpa_credential_exchange(id)
            ON DELETE SET NULL;