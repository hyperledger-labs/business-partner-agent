/*
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
*/

export const schemaAttributeLabel = (key) => {
  if (!key) return "";
  if (key.trim().length > 1) {
    return (
      key.substring(0, 1).toUpperCase() +
      key.substring(1).replace(/([a-z])([A-Z])/g, "$1 $2")
    );
  }
  return key;
};

export const isValidSchemaAttributeName = (value) => (value && /^[a-zA-Z\d-_]+$/.test(value));
