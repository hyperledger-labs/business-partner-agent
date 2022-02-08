/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
export interface StateBpa {
  partners: any[];
  editedDocument: any;
  documents: any[];
  credentials: [];
  schemas: any[];
  proofTemplates: [];
  tags: [];
  partnerSelectList: [];
  credDefSelectList: [];
  busyStack: number;
  expertMode: boolean;
  settings: any;
}

export interface StateMessages {
  messages: any[];
}
