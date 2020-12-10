/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
*/

export const CredentialTypes = Object.freeze({
  PROFILE: {
    type: "ORGANIZATIONAL_PROFILE_CREDENTIAL",
    label: "Organizational Profile",
  },
  SCHEMA_BASED: {
    type: "SCHEMA_BASED",
  },
  UNKNOWN: {
    type: "UNKNOWN",
    label: "Unknown",
  },
});
