CREATE TABLE tag (
    id uuid PRIMARY KEY,
    name character varying(255) NOT NULL,
    is_read_only boolean
);
