/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { CredentialTypes } from "@/constants";
import { StateBpa } from "@/store/state-type";

export const isBusy = (state: StateBpa) => {
  return state.busyStack > 0;
};

export const publicDocumentsAndCredentials = (state: StateBpa) => {
  return [...state.credentials, ...state.documents].filter(
    (d) => d.isPublic === true
  );
};

export const getOrganizationalProfile = (state: StateBpa) => {
  const documents = state.documents.filter(
    (d) => d.type === CredentialTypes.PROFILE.type
  );
  return documents.length === 1 ? documents[0] : undefined;
};

export const getPartners = (state: StateBpa) => {
  return state.partners;
};

export const partnersCount = (state: StateBpa) => {
  return state.partners ? state.partners.length : 0;
};

export const getPartnerByDID = (state: StateBpa) => (did) => {
  return state.partners.find((partner) => {
    return partner.did === did;
  });
};

export const getCredentials = (state: StateBpa) => {
  return state.credentials;
};

export const getProofTemplates = (state: StateBpa) => {
  return state.proofTemplates;
};

export const getPartnerSelectList = (state: StateBpa) => {
  return state.partnerSelectList;
};
