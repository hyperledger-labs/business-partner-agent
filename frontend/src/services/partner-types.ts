/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
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
  valid: boolean;
  ariesSupport: boolean;
  incoming: boolean;
  state: string;
  stateToTimestamp: StateToTimestamp;
  did: string;
  credential?: CredentialEntity[] | null;
  tag?: null[] | null;
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
  credentialData: CredentialData;
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
