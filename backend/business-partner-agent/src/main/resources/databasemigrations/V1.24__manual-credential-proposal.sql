ALTER TABLE bpa_credential_exchange ADD COLUMN credential_proposal jsonb;
ALTER TABLE bpa_credential_exchange ADD COLUMN state_to_timestamp jsonb;
ALTER TABLE bpa_credential_exchange ADD COLUMN error_msg character varying(255);
ALTER TABLE bpa_credential_exchange ALTER COLUMN schema_id DROP NOT NULL;
ALTER TABLE bpa_credential_exchange ALTER COLUMN cred_def_id DROP NOT NULL;