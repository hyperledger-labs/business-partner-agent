/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { components } from "backend-types";

export type RequestIssueCredential =
  components["schemas"]["IssueCredentialRequest"];

export type RequestIssueOobCredential =
  components["schemas"]["IssueOOBCredentialRequest"];

export type RequestCredentialOffer =
  components["schemas"]["CredentialOfferRequest"];

export type RequestCreateCredDef =
  components["schemas"]["CreateCredDefRequest"];

export type RequestCreateSchema = components["schemas"]["CreateSchemaRequest"];

// Issuer controller data types
export type SchemaApi = components["schemas"]["SchemaAPI"];

export type ApiCreateInvitation =
  components["schemas"]["APICreateInvitationResponse"];

export type CredDef = components["schemas"]["CredDef"];

export type CredEx = components["schemas"]["CredEx"];
