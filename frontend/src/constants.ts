/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import i18n from "@/plugins/i18n";
import { ActivityType } from "@/services";
import { TranslateResult } from "vue-i18n";

export const CHAT_CURRENT_USERID = "__self__";

export const CredentialTypes = Object.freeze({
  PROFILE: {
    type: "ORGANIZATIONAL_PROFILE_CREDENTIAL",
  },
  INDY: {
    type: "INDY",
  },
  JSON_LD: {
    type: "JSON_LD",
  },
  UNKNOWN: {
    type: "UNKNOWN",
  },
});

/** API Route paths */
export enum ApiRoutes {
  ADMIN = "/admin",
  ISSUER = "/issuer",
  PARTNERS = "/partners",
  PROOF_EXCHANGES = "/proof-exchanges",
  PROOF_TEMPLATES = "/proof-templates",
  ACTIVITIES = "/activities",
  WALLET = "/wallet",
  STATUS = "/status",
  INVITATIONS = "/invitations",
}

export enum CredentialExchangeRoles {
  ISSUER = "issuer",
  HOLDER = "holder",
}

export enum CredentialExchangeStates {
  PROPOSAL_SENT = "proposal_sent",
  PROPOSAL_RECEIVED = "proposal_received",
  PROBLEM = "problem",
  DECLINED = "declined",
  ABANDONED = "abandoned",
  OFFER_SENT = "offer_sent",
  OFFER_RECEIVED = "offer_received",
  REQUEST_SENT = "request_sent",
  REQUEST_RECEIVED = "request_received",
  CREDENTIAL_ISSUED = "credential_issued",
  CREDENTIAL_RECEIVED = "credential_received",
  CREDENTIAL_ACKED = "credential_acked",
  CREDENTIAL_REVOKED = "credential_revoked",
  DONE = "done",
}

export enum PresentationExchangeStates {
  PROPOSAL_SENT = "proposal_sent",
  PROPOSAL_RECEIVED = "proposal_received",
  REQUEST_SENT = "request_sent",
  REQUEST_RECEIVED = "request_received",
  PRESENTATION_SENT = "presentation_sent",
  PRESENTATION_RECEIVED = "presentation_received",
  VERIFIED = "verified",
  DECLINED = "declined",
  DONE = "done",
  ABANDONED = "abandoned",
  PRESENTATION_ACKED = "presentation_acked",
}

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
  PING_RESPONSE: {
    value: "ping_response",
    label: "Trust Ping active",
  },
  PING_NO_RESPONSE: {
    value: "ping_no_response",
    label: "Trust Ping no response",
  },
});

type ActivityTypesValueLabel = { label: TranslateResult; value: ActivityType };

export const ActivityTypes: Readonly<{
  CREDENTIAL_EXCHANGE: ActivityTypesValueLabel;
  CONNECTION_REQUEST: ActivityTypesValueLabel;
  PRESENTATION_EXCHANGE: ActivityTypesValueLabel;
}> = Object.freeze({
  CONNECTION_REQUEST: {
    value: "CONNECTION_REQUEST",
    label: i18n.t("constants.activityTypes.connectionRequest"),
  },
  CREDENTIAL_EXCHANGE: {
    value: "CREDENTIAL_EXCHANGE",
    label: i18n.t("constants.activityTypes.credentialExchange"),
  },
  PRESENTATION_EXCHANGE: {
    value: "PRESENTATION_EXCHANGE",
    label: i18n.t("constants.activityTypes.presentationExchange"),
  },
});

export const ActivityStates = Object.freeze({
  CONNECTION_REQUEST_ACCEPTED: {
    value: "connection_request_accepted",
    label: i18n.t("constants.activityStates.connectionRequest.accepted"),
  },
  CONNECTION_REQUEST_RECEIVED: {
    value: "connection_request_received",
    label: i18n.t("constants.activityStates.connectionRequest.received"),
  },
  CONNECTION_REQUEST_SENT: {
    value: "connection_request_sent",
    label: i18n.t("constants.activityStates.connectionRequest.sent"),
  },
  CREDENTIAL_EXCHANGE_ACCEPTED: {
    value: "credential_exchange_accepted",
    label: i18n.t("constants.activityStates.credentialExchange.accepted"),
  },
  CREDENTIAL_EXCHANGE_DECLINED: {
    value: "credential_exchange_declined",
    label: i18n.t("constants.activityStates.credentialExchange.declined"),
  },
  CREDENTIAL_EXCHANGE_RECEIVED: {
    value: "credential_exchange_received",
    label: i18n.t("constants.activityStates.credentialExchange.received"),
  },
  CREDENTIAL_EXCHANGE_PROBLEM: {
    value: "credential_exchange_problem",
    label: i18n.t("constants.activityStates.credentialExchange.problem"),
  },
  CREDENTIAL_EXCHANGE_SENT: {
    value: "credential_exchange_sent",
    label: i18n.t("constants.activityStates.credentialExchange.sent"),
  },
  PRESENTATION_EXCHANGE_ACCEPTED: {
    value: "presentation_exchange_accepted",
    label: i18n.t("constants.activityStates.presentationExchange.accepted"),
  },
  PRESENTATION_EXCHANGE_DECLINED: {
    value: "presentation_exchange_declined",
    label: i18n.t("constants.activityStates.presentationExchange.declined"),
  },
  PRESENTATION_EXCHANGE_RECEIVED: {
    value: "presentation_exchange_received",
    label: i18n.t("constants.activityStates.presentationExchange.received"),
  },
  PRESENTATION_EXCHANGE_SENT: {
    value: "presentation_exchange_sent",
    label: i18n.t("constants.activityStates.presentationExchange.sent"),
  },
});

export const ActivityRoles = Object.freeze({
  CONNECTION_REQUEST_SENDER: {
    value: "connection_request_sender",
    label: i18n.t("constants.activityRoles.connectionRequest.sender"),
  },
  CONNECTION_REQUEST_RECIPIENT: {
    value: "connection_request_recipient",
    label: i18n.t("constants.activityRoles.connectionRequest.recipient"),
  },
  CREDENTIAL_EXCHANGE_HOLDER: {
    value: "credential_exchange_holder",
    label: i18n.t("constants.activityRoles.credentialExchange.holder"),
  },
  CREDENTIAL_EXCHANGE_ISSUER: {
    value: "credential_exchange_issuer",
    label: i18n.t("constants.activityRoles.credentialExchange.issuer"),
  },
  PRESENTATION_EXCHANGE_PROVER: {
    value: "presentation_exchange_prover",
    label: i18n.t("constants.activityRoles.presentationExchange.prover"),
  },
  PRESENTATION_EXCHANGE_VERIFIER: {
    value: "presentation_exchange_verifier",
    label: i18n.t("constants.activityRoles.presentationExchange.verifier"),
  },
});

export enum Predicates {
  LESS_THAN_OR_EQUAL_TO = "<=",
  LESS_THAN = "<",
  GREATER_THAN_OR_EQUAL_TO = ">=",
  GREATER_THAN = ">",
}

export const Restrictions = Object.freeze({
  SCHEMA_ID: { value: "schema_id", label: "Schema ID" },
  SCHEMA_NAME: { value: "schema_name", label: "Schema Name" },
  SCHEMA_ISSUER_DID: { value: "schema_issuer_did", label: "Schema Issuer DID" },
  SCHEMA_VERSION: { value: "schema_version", label: "Schema Version" },
  ISSUER_DID: { value: "issuer_did", label: "Issuer DID" },
  CRED_DEF_ID: { value: "cred_def_id", label: "Credential Definition ID" },
});

export const RequestTypes = ["requestedAttributes", "requestedPredicates"];

export enum ExchangeVersion {
  V1 = "V1",
  V2 = "V2",
}
