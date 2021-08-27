ALTER TABLE partner_proof ADD COLUMN exchange_version character varying(255);
ALTER TABLE partner_proof ADD COLUMN state_to_timestamp jsonb;
ALTER TABLE partner_proof ADD COLUMN updated_at timestamp without time zone;
ALTER TABLE my_credential ADD COLUMN exchange_version character varying(255);
ALTER TABLE bpa_credential_exchange ADD COLUMN exchange_version character varying(255);

ALTER TABLE partner_proof DROP COLUMN schema_id;
ALTER TABLE partner_proof DROP COLUMN credential_definition_id;
ALTER TABLE partner_proof DROP COLUMN issued_at;
ALTER TABLE partner_proof DROP COLUMN issuer;
DELETE FROM partner_proof;

ALTER TABLE bpa_credential_exchange DROP COLUMN credential_offer;
ALTER TABLE bpa_credential_exchange DROP COLUMN credential_proposal;