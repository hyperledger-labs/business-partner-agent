ALTER TABLE partner_proof ADD COLUMN proof_request jsonb;
ALTER TABLE partner_proof ADD COLUMN problem_report character varying(255);
ALTER TABLE partner_proof ADD COLUMN thread_id character varying(255);