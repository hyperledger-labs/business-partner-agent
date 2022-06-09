/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { CredDef, PartnerAPI, RuntimeConfig, SchemaAPI } from "@/services";

export interface StateBpa {
  editedDocument: any;
  documents: any[];
  credentials: [];
  proofTemplates: [];
  tags: [];
  partnerSelectList: [];
  busyStack: number;
  expertMode: boolean;
}

export interface IStateSchemas {
  schemas: SchemaAPI[];
}

// TODO
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

export interface IStateCredDefSelectList {
  credDefSelectList: CredDef[];
}

export interface IStatePartners {
  partners: PartnerAPI[];
}

export interface IStateSettings {
  settings: RuntimeConfig;
}

export interface StateMessages {
  messages: any[];
}
