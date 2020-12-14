/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
*/

import { CredentialTypes } from "../constants";

export const getPartnerProfile = (partner) => {
  if (partner && {}.hasOwnProperty.call(partner, "credential")) {
    let partnerProfile = partner.credential.find((cred) => {
      return cred.type === CredentialTypes.PROFILE.type;
    });
    if (partnerProfile) {
      if ({}.hasOwnProperty.call(partnerProfile, "credentialData")) {
        return partnerProfile.credentialData;
      } else if ({}.hasOwnProperty.call(partnerProfile, "documentData")) {
        return partnerProfile.documentData;
      }
    }
  }
  return null;
};

export const getPartnerName = (partner) => {
  let credential = partner;
  if (
    {}.hasOwnProperty.call(partner, "credential") &&
    Array.isArray(partner.credential)
  ) {
    credential = partner.credential.find((p) => {
      return p.type === CredentialTypes.PROFILE.type;
    });
  }

  if (typeof credential !== "object") {
    return "";
  }

  if (
    {}.hasOwnProperty.call(credential, "credentialData") &&
    {}.hasOwnProperty.call(credential.credentialData, "legalName")
  ) {
    return credential.credentialData.legalName;
  }

  return "";
};
