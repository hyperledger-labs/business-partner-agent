-- bpa_cred_def constraints

ALTER TABLE bpa_cred_def
    ADD CONSTRAINT bpa_cred_def_schema_fk_1
        FOREIGN KEY (schema_id)
            REFERENCES bpaschema(id);

-- bpa_restrictions constraints

ALTER TABLE bpa_restrictions
    ADD CONSTRAINT bpa_restrictions_schema_fk_1
        FOREIGN KEY (schema_id)
            REFERENCES bpaschema(id);

-- delete read only columns

ALTER TABLE bpaschema DROP COLUMN is_read_only;
ALTER TABLE bpa_restrictions DROP COLUMN is_read_only;

-- bpa_credential_exchange constraints

-- ALTER TABLE bpa_credential_exchange
--     ADD CONSTRAINT bpa_credential_exchange_partner_fk_1
--         FOREIGN KEY (partner_id)
--             REFERENCES partner(id)
--             ON DELETE SET NULL;
--
-- ALTER TABLE bpa_credential_exchange
--     ADD CONSTRAINT bpa_credential_exchange_restriction_fk_1
--         FOREIGN KEY (cred_def_id)
--             REFERENCES bpa_restrictions(id);
--
-- ALTER TABLE bpa_credential_exchange
--     ADD CONSTRAINT bpa_credential_exchange_schema_fk_1
--         FOREIGN KEY (schema_id)
--             REFERENCES bpaschema(id);