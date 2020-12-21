ALTER TABLE my_credential ADD COLUMN issuer character varying(255);
ALTER TABLE my_credential ALTER COLUMN connection_id DROP NOT NULL;