ALTER TABLE my_document ADD COLUMN label character varying(255);
ALTER TABLE my_credential ADD COLUMN label character varying(255);
ALTER TABLE my_credential ALTER COLUMN type DROP NOT NULL;
ALTER TABLE partner_proof ALTER COLUMN issued_at DROP NOT NULL;
ALTER TABLE partner_proof ALTER COLUMN type DROP NOT NULL;
ALTER TABLE partner_proof ALTER COLUMN proof DROP NOT NULL;