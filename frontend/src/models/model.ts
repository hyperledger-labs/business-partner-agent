/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
export const profileModel = {
  type: "Legal Entity",
  legalName: "",
  altName: "",
  identifier: [
    {
      id: "",
      type: "",
    },
  ],
  registeredSite: {
    address: {
      streetAddress: "",
      zipCode: "",
      city: "",
      country: "",
      region: "",
    },
  },
};
