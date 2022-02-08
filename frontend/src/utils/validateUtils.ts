/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * Validates a JSON including prevention of false values caused by JSON.parse
 * @param {String} jsonString The JSON string which is validated
 * @returns {Boolean} Returns true if JSON is valid, else false
 */
export const validateJson = (jsonString: string): boolean => {
  try {
    const json = JSON.parse(jsonString);

    if (json && typeof json === "object") {
      return true;
    }
  } catch {
    return false;
  }

  return false;
};
