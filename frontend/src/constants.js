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
  ISSUER: '/issuer'
});

export const CredentialExchangeRoles = Object.freeze( {
  ISSUER: 'ISSUER',
  HOLDER: 'HOLDER'
});

export const CredentialExchangeStates = Object.freeze( {
  PROPOSAL_SENT: 'proposal_sent',
  PROPOSAL_RECEIVED: 'proposal_received',
  OFFER_SENT: 'offer_sent',
  OFFER_RECEIVED: 'offer_received',
  REQUEST_SENT: 'request_sent',
  REQUEST_RECEIVED: 'request_received',
  CREDENTIAL_ISSUED: 'credential_issued',
  CREDENTIAL_RECEIVED: 'credential_received',
  CREDENTIAL_ACKED: 'credential_acked'
});

