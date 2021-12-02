ALTER TABLE bpaschema ADD COLUMN type character varying(255) NOT NULL DEFAULT 'INDY';
ALTER TABLE bpaschema ADD COLUMN ld_type character varying(255);
ALTER TABLE bpaschema ALTER COLUMN seq_no DROP NOT NULL;
ALTER TABLE bpaschema DROP CONSTRAINT bpaschema_schema_id_key;