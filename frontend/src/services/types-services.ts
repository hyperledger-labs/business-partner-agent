/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { components } from "backend-types";

// Request types
export type IssueCredentialRequestIndy =
  components["schemas"]["IssueCredentialRequest.IssueIndyCredentialRequest"];

export type IssueCredentialRequestJsonLd =
  components["schemas"]["IssueCredentialRequest.IssueLDCredentialRequest"];

export type IssueOobCredentialRequest =
  components["schemas"]["IssueOOBCredentialRequest"];

export type CredentialOfferRequest =
  components["schemas"]["CredentialOfferRequest"];

export type CreateCredDefRequest =
  components["schemas"]["CreateCredDefRequest"];

export type CreateSchemaRequest = components["schemas"]["CreateSchemaRequest"];

export type AddTagRequest = components["schemas"]["AddTagRequest"];

export type SendMessageRequest = components["schemas"]["SendMessageRequest"];

export type SendProofRequest = components["schemas"]["SendProofRequest"];

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

export type MyDocumentAPI = components["schemas"]["MyDocumentAPI"];

export type AriesCredential = components["schemas"]["AriesCredential"];

export type RuntimeConfig = components["schemas"]["RuntimeConfig"];

// Page response from server
export class Page<T> {
  size?: number;
  totalPages?: number;
  totalSize?: number;
  pageNumber?: number;
  numberOfElements?: number;
  content?: T;
}

// Translates between datatable pagination names and server names, see also PaginationCommand
export class PageOptions {
  page = 1;
  itemsPerPage = 10;
  sortBy: string[] = [];
  sortDesc: boolean[] = [];

  static toUrlSearchParams(options: PageOptions = new PageOptions()) {
    const params = new URLSearchParams();
    const optionKeys = Object.keys(options).length;
    const currentPage = optionKeys > 0 ? Number(options.page) - 1 : 0;
    params.append("page", currentPage.toString());
    if (options.itemsPerPage) {
      params.append("size", options.itemsPerPage.toString());
    }
    if (options.sortBy && options.sortBy.length > 0) {
      params.append("q", String(options.sortBy));
    }
    if (options.sortDesc) {
      params.append("desc", String(options.sortDesc));
    }
    return params;
  }
}
