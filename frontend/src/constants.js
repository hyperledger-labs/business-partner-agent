/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
*/

export const CredentialTypes = Object.freeze({
  PROFILE: {
    type: "ORGANIZATIONAL_PROFILE_CREDENTIAL",
    label: "Organizational Profile",
  },
  SCHEMA_BASED: {
    type: "SCHEMA_BASED",
  },
  UNKNOWN: {
    type: "UNKNOWN",
    label: "Unknown",
  },
});

/** API Route paths */
export const ApiRoutes = Object.freeze({
  ADMIN: "/admin",
  ISSUER: "/issuer",
  PARTNERS: "/partners",
});

export const CredentialExchangeRoles = Object.freeze({
  ISSUER: "ISSUER",
  HOLDER: "HOLDER",
});

export const CredentialExchangeStates = Object.freeze({
  PROPOSAL_SENT: "proposal_sent",
  PROPOSAL_RECEIVED: "proposal_received",
  OFFER_SENT: "offer_sent",
  OFFER_RECEIVED: "offer_received",
  REQUEST_SENT: "request_sent",
  REQUEST_RECEIVED: "request_received",
  CREDENTIAL_ISSUED: "credential_issued",
  CREDENTIAL_RECEIVED: "credential_received",
  CREDENTIAL_ACKED: "credential_acked",
});

export const PartnerStates = Object.freeze({
  INVITATION: {
    value: "invitation",
    label: "invitation",
  },
  REQUEST: {
    value: "request",
    label: "request",
  },
  INACTIVE: {
    value: "inactive",
    label: "inacitve",
  },
  ACTIVE: {
    value: "active",
    label: "active",
  },
  COMPLETED: {
    value: "completed",
    label: "completed",
  },
  RESPONSE: {
    value: "response",
    label: "response",
  },
  ACTIVE_OR_RESPONSE: {
    value: "active_response",
    label: "active",
  },
  CONNECTION_REQUEST_SENT: {
    value: "conn_request_sent",
    label: "Connection request sent",
  },
  CONNECTION_REQUEST_RECEIVED: {
    value: "conn_request_received",
    label: "Connection request received",
  },
});
