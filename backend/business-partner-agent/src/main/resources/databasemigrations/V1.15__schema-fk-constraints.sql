-- fix broken records

DELETE FROM bpa_cred_def AS creds
WHERE NOT EXISTS (
        SELECT FROM bpaschema schema
        WHERE schema.id = creds.schema_id
    );

DELETE FROM bpa_credential_exchange AS ex
WHERE NOT EXISTS (
        SELECT FROM bpaschema schema
        WHERE schema.id = ex.schema_id
    );

DELETE FROM bpa_credential_exchange AS ex
WHERE NOT EXISTS (
        SELECT FROM partner p
        WHERE p.id = ex.partner_id
    );

DELETE FROM bpa_restrictions AS res
WHERE NOT EXISTS (
        SELECT FROM bpaschema schema
        WHERE schema.id = res.schema_id
    );

-- delete read only columns

ALTER TABLE bpaschema DROP COLUMN is_read_only;
ALTER TABLE bpa_restrictions DROP COLUMN is_read_only;

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

-- bpa_credential_exchange constraints

ALTER TABLE bpa_credential_exchange
    ADD CONSTRAINT bpa_credential_exchange_partner_fk_1
        FOREIGN KEY (partner_id)
            REFERENCES partner(id)
            ON DELETE CASCADE;

ALTER TABLE bpa_credential_exchange
    ADD CONSTRAINT bpa_credential_exchange_bpa_cred_def_fk_1
        FOREIGN KEY (cred_def_id)
            REFERENCES bpa_cred_def(id);

ALTER TABLE bpa_credential_exchange
    ADD CONSTRAINT bpa_credential_exchange_schema_fk_1
        FOREIGN KEY (schema_id)
            REFERENCES bpaschema(id);