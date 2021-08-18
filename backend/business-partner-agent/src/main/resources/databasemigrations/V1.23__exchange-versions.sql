ALTER TABLE partner_proof ADD COLUMN exchange_version character varying(255);
ALTER TABLE my_credential ADD COLUMN exchange_version character varying(255);
ALTER TABLE bpa_credential_exchange ADD COLUMN exchange_version character varying(255);