/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { CredDef } from "@/services";

export interface StateBpa {
  partners: any[];
  editedDocument: any;
  documents: any[];
  credentials: [];
  schemas: any[];
  proofTemplates: [];
  tags: [];
  partnerSelectList: [];
  busyStack: number;
  expertMode: boolean;
  settings: any;
}

export interface IStateCredDefSelectList {
  credDefSelectList: CredDef[];
}

export interface StateMessages {
  messages: any[];
}
