/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { components } from "backend-types";

// Request types
export type IssueCredentialRequest =
  components["schemas"]["IssueCredentialRequest"];

export type IssueOobCredentialRequest =
  components["schemas"]["IssueOOBCredentialRequest"];

export type CredentialOfferRequest =
  components["schemas"]["CredentialOfferRequest"];

export type CreateCredDefRequest =
  components["schemas"]["CreateCredDefRequest"];

export type CreateSchemaRequest = components["schemas"]["CreateSchemaRequest"];

export type AddTagRequest = components["schemas"]["AddTagRequest"];

export type SendMessageRequest = components["schemas"]["SendMessageRequest"];

export type AddSchemaRequest = components["schemas"]["AddSchemaRequest"];

export type UpdatePartnerRequest =
  components["schemas"]["UpdatePartnerRequest"];

export type DeclineExchangeRequest =
  components["schemas"]["DeclineExchangeRequest"];

export type ApproveProofRequest = components["schemas"]["ApproveProofRequest"];

export type PresentationRequestCredentials =
  components["schemas"]["PresentationRequestCredentials"];

// Data and response types
export type SchemaApi = components["schemas"]["SchemaAPI"];

export type TagApi = components["schemas"]["TagAPI"];

export type PartnerAPI = components["schemas"]["PartnerAPI"];

export type ApiCreateInvitation =
  components["schemas"]["APICreateInvitationResponse"];

export type CredDef = components["schemas"]["CredDef"];

export type CredEx = components["schemas"]["CredEx"];

export type Tag = components["schemas"]["Tag"];

export type ChatMessage = components["schemas"]["ChatMessage"];

export type AriesProofExchange = components["schemas"]["AriesProofExchange"];

export type ProofTemplate = components["schemas"]["ProofTemplate"];

export type PresentationRequestVersion =
  components["schemas"]["PresentationRequestVersion"];
