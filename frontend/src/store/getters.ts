/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { CredentialTypes } from "@/constants";

export const istBusy = (state) => {
  return state.busyStack > 0;
};

export const publicDocumentsAndCredentials = (state) => {
  return [...state.credentials, ...state.documents].filter(
    (d) => d.isPublic === true
  );
};

export const getOrganizationalProfile = (state) => {
  const documents = state.documents.filter(
    (d) => d.type === CredentialTypes.PROFILE.type
  );
  return documents.length === 1 ? documents[0] : undefined;
};

export const getPartners = (state) => {
  return state.partners;
};

export const partnersCount = (state) => {
  return state.partners ? state.partners.length : 0;
};

export const getPartnerByDID = (state) => (did) => {
  return state.partners.find((partner) => {
    return partner.did === did;
  });
};

export const getSchemas = (state) => {
  return state.schemas;
};

export const getCredentials = (state) => {
  return state.credentials;
};

export const getSchemaBasedSchemas = (state) => {
  return state.schemas.filter((schema) => {
    return (
      schema.type === CredentialTypes.INDY.type ||
      schema.type === CredentialTypes.JSON_LD.type
    );
  });
};

export const getSchemaById = (state) => (schemaId) => {
  if (!schemaId) {
    return;
  }
  return state.schemas.find((schema) => {
    return schema.schemaId === schemaId;
  });
};

export const getSchemaByType = (state) => (schemaType) => {
  if (!schemaType) {
    return;
  }
  return state.schemas.find((schema) => {
    return schema.type === schemaType;
  });
};

export const getSchemaLabel = (state) => (typeName, schemaId) => {
  let schemaType = { label: "" };
  if (schemaId) {
    schemaType = state.schemas.find((schema) => {
      return schema.schemaId === schemaId;
    });
  } else if (typeName) {
    schemaType = state.schemas.find((schema) => {
      return schema.type === typeName;
    });
  }

  if (schemaType && Object.prototype.hasOwnProperty.call(schemaType, "label")) {
    return schemaType.label;
  }

  if (schemaId) {
    //Maybe in backend get label for schemaId
    return schemaId;
  }
  return "";
};

export const getSettingByKey = (state) => (key) => {
  if (
    state.settings &&
    Object.prototype.hasOwnProperty.call(state.settings, key)
  ) {
    return state.settings[key];
  }
};

export const getSettings = (state) => {
  return state.settings;
};

export const getProofTemplates = (state) => {
  return state.proofTemplates;
};

export const getPartnerSelectList = (state) => {
  return state.partnerSelectList;
};

// eslint-disable-next-line unicorn/prevent-abbreviations
export const getCredDefSelectList = (state) => {
  return state.credDefSelectList;
};
