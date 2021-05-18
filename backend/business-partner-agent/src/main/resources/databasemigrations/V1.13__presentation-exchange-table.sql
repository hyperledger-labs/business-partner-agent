CREATE TABLE bpa_presentation_exchange (
    id uuid PRIMARY KEY,
    created_at timestamp without time zone,
    partner_id uuid NOT NULL, -- partner table id (-> connection id)
    label character varying(255),
    thread_id character varying(255) NOT NULL,
    presentation_exchange_id character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    presentation jsonb,
    presentation_proposal_dict jsonb,
    presentation_request jsonb,
    presentation_request_dict jsonb,
    updated_at timestamp without time zone,


    FOREIGN KEY (partner_id) REFERENCES partner(id) DEFERRABLE INITIALLY DEFERRED



);

