create or replace view activity_vw as
select t.* from
(select p.id as partner_id, 'CONNECTION_REQUEST' as type, p.id, p.state, case when p.incoming then 'CONNECTION_REQUEST_RECIPIENT' else 'CONNECTION_REQUEST_SENDER' end as role, p.updated_at  from partner as p
union
select pp.partner_id, 'PRESENTATION_EXCHANGE' as type, pp.id, pp.state, pp.role, case when pp.issued_at is not null then pp.issued_at else pp.created_at end as updated_at from partner_proof as pp
union
select bce.partner_id, 'CREDENTIAL_EXCHANGE' as type, bce.id, bce.state, bce.role, bce.updated_at from bpa_credential_exchange as bce
union
select mcp.id as partner_id, 'CREDENTIAL_EXCHANGE' as type, mc.id, mc.state, 'HOLDER' as role, mc.issued_at as updated_at from my_credential as mc inner join partner as mcp on mc.connection_id = mcp.connection_id) as t
order by t.updated_at desc;
