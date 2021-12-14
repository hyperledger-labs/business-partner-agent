ALTER TABLE bpaschema ADD COLUMN type character varying(255) NOT NULL DEFAULT 'INDY';
ALTER TABLE bpaschema ADD COLUMN ld_type character varying(255);
ALTER TABLE bpaschema ALTER COLUMN seq_no DROP NOT NULL;
ALTER TABLE bpaschema DROP CONSTRAINT bpaschema_schema_id_key;
ALTER TABLE partner ADD COLUMN invitation_record jsonb;

ALTER TABLE my_document ADD COLUMN fk_schema_id uuid;
ALTER TABLE my_document
    ADD CONSTRAINT bpa_document_schema_fk_1
        FOREIGN KEY (fk_schema_id)
            REFERENCES bpaschema(id);