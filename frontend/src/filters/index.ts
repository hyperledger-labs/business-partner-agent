/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import moment from "moment";

/**
 * @function formatDate
 * Converts a date to an 'YYYY-MM-DD' formatted string
 * @param {Date, String, Long} value A date object
 * @returns {String} A string representation of `value`
 */
export function formatDate(value: string): string {
  if (value) {
    return moment(value).format("YYYY-MM-DD");
  }
}

/**
 * @function formatDateLong
 * Converts a date to an 'YYYY-MM-DD HH:mm' formatted string
 * @param {Date, String, Long} value A date object
 * @returns {String} A string representation of `value`
 */
export function formatDateLong(value: string | number) {
  if (value) {
    return moment(value).format("YYYY-MM-DD HH:mm");
  }
}

export function credentialTag(credDefinitionId: string) {
  if (!credDefinitionId) return "";
  const pos = credDefinitionId.lastIndexOf(":");
  return credDefinitionId.slice(Math.max(0, pos + 1));
}

export function capitalize(string: string) {
  return string && string !== ""
    ? string.replace(/\w\S*/g, (w) => w.replace(/^\w/, (c) => c.toUpperCase()))
    : "";
}

// Define Global Vue Filters
//
// {{ expression | filter }}
//
const filters = {
  formatDate(value: string) {
    return formatDate(value);
  },
  formatDateLong(value: string | number) {
    return formatDateLong(value);
  },
  credentialTag(credentialDefinitionId: string) {
    return credentialTag(credentialDefinitionId);
  },
  capitalize(string: string) {
    return capitalize(string);
  },
};

export default filters;
