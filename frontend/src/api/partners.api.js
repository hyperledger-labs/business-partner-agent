/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
*/

import httpClient from "./httpClient";

const END_POINT = "/partners";

const getAllPartners = () => httpClient.get(END_POINT);

const getPartner = (partnerId) => httpClient.get(`${END_POINT}/${partnerId}`);

const updatePartner = (partnerId, payload) =>
  httpClient.put(`${END_POINT}/${partnerId}`, payload);

const removePartner = (partnerId) =>
  httpClient.delete(`${END_POINT}/${partnerId}`);

const lookupPartner = (did) => httpClient.get(`${END_POINT}/lookup/${did}`);

const refreshPartner = (partnerId) =>
  httpClient.get(`${END_POINT}/${partnerId}/refresh`);

const getIssuerForSchema = (schemaId) =>
  httpClient.get(`${END_POINT}?issuerFor=${schemaId}`);

export {
  getAllPartners,
  getPartner,
  updatePartner,
  removePartner,
  lookupPartner,
  refreshPartner,
  getIssuerForSchema,
};
