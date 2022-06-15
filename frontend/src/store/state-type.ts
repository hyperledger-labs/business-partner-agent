/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  AriesCredential,
  CredDef,
  MyDocumentAPI,
  PartnerAPI,
  ProofTemplate,
  RuntimeConfig,
  SchemaAPI,
  TagAPI,
} from "@/services";

export interface IStateExpertMode {
  isExpert: boolean;
}

export interface IStatePartnerSelectList {
  partnerSelectList: PartnerAPI[];
}

export interface IStateSchemas {
  schemaList: SchemaAPI[];
}

// TODO: Find out types
export interface IStateNotifications {
  activityNotifications: any;
  credentialNotifications: any;
  partnerNotifications: any;
  presentationNotifications: any;
  taskNotifications: any;
}

export interface IStateSocketEvents {
  socket: {
    isConnected: boolean;
    message: string;
    reconnectError: boolean;
  };
}

export interface IStateTransactionAuthorAgreement {
  taaRequired: boolean;
  taaText: string;
  taaDigest: string;
  taaVersion: string;
  taaLoaded: boolean;
}

export interface IStateCredentialDefinitions {
  credDefSelectList: CredDef[];
}

export interface IStatePartners {
  partnerList: PartnerAPI[];
}

export interface IStateCredentialsAndDocuments {
  credentialList: AriesCredential[];
  documentList: MyDocumentAPI[];
}

export interface IStateProofTemplates {
  proofTemplateList: ProofTemplate[];
}

export interface IStateSettings {
  config: RuntimeConfig;
}

export interface IStateTags {
  tagList: TagAPI[];
}

export interface IStateChat {
  messages: any[];
}
