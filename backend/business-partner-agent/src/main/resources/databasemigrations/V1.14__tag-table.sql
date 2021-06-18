CREATE TABLE tag (
    id uuid PRIMARY KEY,
    name character varying(255) NOT NULL UNIQUE,
    is_read_only boolean
);

CREATE TABLE partner_tag (
    partner_id uuid,
    tag_id uuid,
    PRIMARY KEY (partner_id, tag_id),
    CONSTRAINT partner_tag_fk_1
        FOREIGN KEY(partner_id) REFERENCES partner,
    CONSTRAINT partner_tag_fk_2
        FOREIGN KEY(tag_id) REFERENCES tag
);
