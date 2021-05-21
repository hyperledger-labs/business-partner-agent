ALTER TABLE partner_proof ADD COLUMN proof_request jsonb;

ALTER TABLE partner_proof ADD CONSTRAINT partner_proof_partner_id_fk FOREIGN KEY (partner_id) REFERENCES partner(id);
