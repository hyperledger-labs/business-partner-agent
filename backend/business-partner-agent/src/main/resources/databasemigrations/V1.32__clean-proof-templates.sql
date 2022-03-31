DELETE FROM partner_proof WHERE proof_template_id IN (SELECT id FROM bpa_proof_template);
DELETE FROM bpa_proof_template;