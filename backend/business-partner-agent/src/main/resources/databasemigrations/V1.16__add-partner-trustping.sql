ALTER TABLE partner ADD COLUMN trust_ping boolean;
ALTER TABLE partner ADD COLUMN invitation_msg_id character varying(255);
UPDATE partner set trust_ping = true;