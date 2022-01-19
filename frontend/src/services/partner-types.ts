/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
export interface Partner {
  id: string;
  createdAt: number;
  updatedAt: number;
  lastSeen: number;
  trustPing: boolean;
  valid?: boolean;
  ariesSupport: boolean;
  incoming: boolean;
  state: string; // Connection state
  stateToTimestamp: StateToTimestamp;
  alias: string;
  did: string;
  credential?: CredentialEntity[]; // Needs to be revised in the backend
  tag: Tag[];
  name: string;
}

export interface StateToTimestamp {
  request: number;
  response: number;
  completed: number;
}

export interface CredentialEntity {
  type: string;
  typeLabel: string;
  indyCredential: boolean;
  issuer: string;
  schemaId: string;
  credentialData: CredentialData;
}

export interface MyDocumentApi {
  id: string;
  createdDate: number;
  updatedDate: number;
  type: string;
  typeLabel: string;
  schemaId: string;
  isPublic: boolean;
  label: string;
  documentData: never;
}

export interface CredentialData {
  id: string;
  type: string;
  altName: string;
  legalName: string;
  identifier?: IdentifierEntity[] | null;
  registeredSite: RegisteredSite;
}

export interface IdentifierEntity {
  id: string;
  type: string;
}

export interface RegisteredSite {
  address: Address;
}

export interface Address {
  city: string;
  region: string;
  country: string;
  zipCode: string;
  streetAddress: string;
}

export interface Tag {
  id: string;
  isReadOnly: boolean;
  name: string;
}

export interface UpdatePartnerRequest {
  alias: string;
  tag: Tag[];
  trustPing: boolean;
}

export interface DeclineExchangeRequest {
  message: string;
}

export interface ChatMessage {
  id: string;
  content: string;
  incoming: boolean;
  createdAtTs: number;
  partner: Partner;
}
