ALTER TABLE partner ADD COLUMN trust_ping boolean;
UPDATE partner set trust_ping = true;