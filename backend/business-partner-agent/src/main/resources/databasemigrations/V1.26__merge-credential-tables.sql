ALTER TABLE bpa_credential_exchange ADD COLUMN is_public boolean;
ALTER TABLE bpa_credential_exchange ADD COLUMN issuer character varying(255);
ALTER TABLE bpa_credential_exchange ADD COLUMN referent character varying(255);

ALTER TABLE bpa_credential_exchange ALTER COLUMN partner_id DROP NOT NULL;