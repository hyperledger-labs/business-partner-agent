ALTER TABLE bpa_credential_exchange ADD COLUMN is_public boolean;
ALTER TABLE bpa_credential_exchange ADD COLUMN issuer character varying(255);
ALTER TABLE bpa_credential_exchange ADD COLUMN referent character varying(255);

-- insert into bpa_credential_exchange select * from my_credential;

-- drop table my_credential;
-- partner set null