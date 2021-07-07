/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
*/

import { CredentialTypes, PartnerStates } from "../constants";

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
  if (typeof partner !== "object") {
    return "";
  } else if ({}.hasOwnProperty.call(partner, "alias")) {
    return partner.alias;
  } else if (
    {}.hasOwnProperty.call(partner, "profile") &&
    partner.profile !== null &&
    {}.hasOwnProperty.call(partner.profile, "legalName")
  ) {
    return partner.profile.legalName;
  } else {
    const profile = getPartnerProfile(partner);
    if (profile && {}.hasOwnProperty.call(profile, "legalName")) {
      return profile.legalName;
    } else {
      return partner.did;
    }
  }
};

export const getPartnerState = (partner) => {
  if ({}.hasOwnProperty.call(partner, "state")) {
    if (partner.state === PartnerStates.REQUEST.value) {
      if (partner.incoming) {
        return PartnerStates.CONNECTION_REQUEST_RECEIVED;
      } else {
        return PartnerStates.CONNECTION_REQUEST_SENT;
      }
    } else if (
      partner.state ===
      (PartnerStates.ACTIVE.value || PartnerStates.RESPONSE.value || PartnerStates.COMPLETED.value)
    ) {
      return PartnerStates.ACTIVE_OR_RESPONSE;
    } else {
      return Object.values(PartnerStates).find((state) => {
        return partner.state === state.value;
      });
    }
  } else {
    return {
      value: "",
      label: "",
    };
  }
};

export const getPartnerStateColor = (state) => {
  if (state === PartnerStates.REQUEST.value) {
    return "yellow";
  } else if (state === PartnerStates.ABANDONED.value) {
    return "red";
  } else if (
    state === PartnerStates.ACTIVE.value ||
    state === PartnerStates.RESPONSE.value
  ) {
    return "green";
  } else {
    return "grey";
  }
};
