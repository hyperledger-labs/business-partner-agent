CREATE TABLE activity (
    id uuid PRIMARY KEY,
    partner_id uuid NOT NULL,
    link_id uuid NOT NULL,
    type character varying(255),
    role character varying(255),
    state character varying(255),
    completed boolean NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);
