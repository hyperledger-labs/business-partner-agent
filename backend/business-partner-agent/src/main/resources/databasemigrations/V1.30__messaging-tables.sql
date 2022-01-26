CREATE TABLE message_template (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    subject character varying(255),
    template character varying(2048) NOT NULL
);

CREATE TABLE message_user_info (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    type character varying(255) NOT NULL,
    send_to character varying(255) NOT NULL,
    label character varying(255)
);

CREATE TABLE message_trigger_config (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    trigger character varying(2048) NOT NULL,
    template_id uuid,
    user_info_id uuid NOT NULL
);

-- add foreign keys
ALTER TABLE message_trigger_config
    ADD CONSTRAINT message_template_fk_1
        FOREIGN KEY (template_id)
            REFERENCES message_template(id);


ALTER TABLE message_trigger_config
    ADD CONSTRAINT message_user_info_fk_1
        FOREIGN KEY (user_info_id)
            REFERENCES message_user_info(id);

-- clean up table names
ALTER TABLE bpaschema RENAME TO bpa_schema;
ALTER TABLE bpastate RENAME TO bpa_state;
ALTER TABLE bpauser RENAME TO bpa_user;
ALTER TABLE bpawebhook RENAME TO bpa_webhook;