ALTER TABLE bpa_credential_exchange ADD COLUMN cred_rev_id character varying(255);
ALTER TABLE bpa_credential_exchange ADD COLUMN rev_reg_id character varying(255);
ALTER TABLE bpa_credential_exchange ADD COLUMN revoked boolean;

ALTER TABLE my_credential ADD COLUMN revoked boolean;

UPDATE my_credential set type = 'INDY' WHERE type = 'SCHEMA_BASED';
UPDATE my_document SET type = 'INDY' WHERE type = 'SCHEMA_BASED';

-- clean up events in old format
DELETE FROM message_queue;