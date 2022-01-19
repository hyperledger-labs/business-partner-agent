/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */

export const schemaAttributeLabel = (key: string): string => {
  if (!key) return "";
  if (key.trim().length > 1) {
    return (
      key.slice(0, 1).toUpperCase() +
      key.slice(1).replace(/([a-z])([A-Z])/g, "$1 $2")
    );
  }
  return key;
};

export const isValidSchemaName = (value: string): boolean =>
  value && /^[A-Za-z\d-_]+$/.test(value);

export const isValidSchemaAttributeName = (value: string): boolean =>
  value && /^[_a-z]+$/.test(value);

export const isValidSchemaVersion = (value: string): boolean =>
  value && /^(\d+)\.(\d+)(?:\.\d+)?$/.test(value);

export const getBooleanFromString = (
  inputString: string
): boolean | undefined => {
  if (inputString === "true") {
    return true;
  } else if (inputString === "false") {
    return false;
  } else {
    return;
  }
};
