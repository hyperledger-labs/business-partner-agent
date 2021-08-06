ALTER TABLE bpa_credential_exchange ADD COLUMN cred_rev_id character varying(255);
ALTER TABLE bpa_credential_exchange ADD COLUMN rev_reg_id character varying(255);
ALTER TABLE bpa_credential_exchange ADD COLUMN revoked boolean;

ALTER TABLE my_credential ADD COLUMN revoked boolean;