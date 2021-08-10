ALTER TABLE activity
    ADD CONSTRAINT activity_partner_fk_1
        FOREIGN KEY (partner_id)
            REFERENCES partner(id)
            ON DELETE CASCADE;

ALTER TABLE chat_message
    ADD CONSTRAINT chat_message_partner_fk_1
        FOREIGN KEY (partner_id)
            REFERENCES partner(id)
            ON DELETE CASCADE;
