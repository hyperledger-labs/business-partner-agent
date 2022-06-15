ALTER TABLE partner_proof ADD COLUMN type character varying(255) NOT NULL DEFAULT 'INDY';
ALTER TABLE bpa_proof_template ADD COLUMN type character varying(255) NOT NULL DEFAULT 'INDY';
ALTER TABLE bpa_credential_exchange DROP COLUMN issuer;