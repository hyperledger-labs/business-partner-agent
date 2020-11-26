ALTER TABLE my_document ADD COLUMN schema_id character varying(255);

UPDATE my_document SET type = 'INDY_CREDENTIAL' WHERE type = 'OTHER' OR type = 'COMMERCIAL_REGISTER_CREDENTIAL' OR type = 'BANK_ACCOUNT_CREDENTIAL';
ALTER TABLE partner_proof DROP COLUMN type;
UPDATE my_credential SET type = 'INDY_CREDENTIAL';
ALTER TABLE bpaschema DROP COLUMN type;