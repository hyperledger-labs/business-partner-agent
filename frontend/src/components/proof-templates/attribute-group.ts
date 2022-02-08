/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
export interface AttributeGroup {
  attributes: [];
  ui: {
    selectedAttributes: [];
    selectedRestrictionsByTrustedIssuer: [];
    predicateConditionsErrorCount: number;
  };
}

export interface SchemaLevelRestriction {
  schemaId: string;
  schemaName: string;
  schemaVersion: string;
  schemaIssuerDid: string;
  issuerDid: string;
  credentialDefinitionId: string;
}
