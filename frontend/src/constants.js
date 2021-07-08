/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
*/

import i18n from '@/plugins/i18n';

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
    label: "inactive",
  },
  ABANDONED: {
    value: "abandoned",
    label: "abandoned",
  },
  ACTIVE: {
    value: "active",
    label: "active",
  },
  COMPLETED: {
    value: "completed",
    label: "active",
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

export const ActivityTypes = Object.freeze({
  CONNECTION_INVITATION: {
    value: "connection_invitation",
    label: i18n.t('constants.activityTypes.connectionInvitation'),
  },
  CREDENTIAL_OFFER: {
    value: "credential_offer",
    label: i18n.t('constants.activityTypes.credentialOffer'),
  },
  PRESENTATION_EXCHANGE: {
    value: "presentation_exchange",
    label: i18n.t('constants.activityTypes.presentationExchange'),
  },
});

export const ActivityStates = Object.freeze({
  CONNECTION_REQUEST_ACCEPTED: {
    value: "connection_request_accepted",
    label: i18n.t('constants.activityStates.connectionRequestAccepted'),
  },
  CONNECTION_REQUEST_RECEIVED: {
    value: "connection_request_received",
    label: i18n.t('constants.activityStates.connectionRequestReceived'),
  },
  CONNECTION_REQUEST_SENT: {
    value: "connection_request_sent",
    label: i18n.t('constants.activityStates.connectionRequestSent'),
  },
  PRESENTATION_EXCHANGE_ACCEPTED: {
    value: "presentation_exchange_accepted",
    label: i18n.t('constants.activityStates.presentationExchangeAccepted'),
  },
  PRESENTATION_EXCHANGE_RECEIVED: {
    value: "presentation_exchange_received",
    label: i18n.t('constants.activityStates.presentationExchangeReceived'),
  },
  PRESENTATION_EXCHANGE_SENT: {
    value: "presentation_exchange_sent",
    label: i18n.t('constants.activityStates.presentationExchangeSent'),
  },

});

export const ActivityRoles = Object.freeze({
  CONNECTION_INVITATION_SENDER: {
    value: "connection_invitation_sender",
    label: i18n.t('constants.activityRoles.connectionInvitationSender'),
  },
  CONNECTION_INVITATION_RECIPIENT: {
    value: "connection_invitation_recipient",
    label: i18n.t('constants.activityRoles.connectionInvitationRecipient'),
  },
  PRESENTATION_EXCHANGE_PROVER: {
    value: "presentation_exchange_prover",
    label: i18n.t('constants.activityRoles.presentationExchangeProver'),
  },
  PRESENTATION_EXCHANGE_VERIFIER: {
    value: "presentation_exchange_verifier",
    label: i18n.t('constants.activityRoles.presentationExchangeVerifier'),
  },

});

