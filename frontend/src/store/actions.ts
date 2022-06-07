/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { PartnerStates } from "@/constants";
import { EventBus, axios, apiBaseUrl } from "../main";
import { getPartnerProfile } from "@/utils/partnerUtils";
import adminService from "@/services/admin-service";
import proofTemplateService from "@/services/proof-template-service";
import partnerService from "@/services/partner-service";
import {
  AriesCredential,
  MyDocumentAPI,
  Page,
  ProofTemplate,
} from "@/services";
import { AxiosResponse } from "axios";

export const loadTags = async ({ commit }) => {
  adminService
    .listTags()
    .then((result) => {
      const tags = result.data;
      console.log(tags);
      commit({
        type: "setTags",
        tags: tags,
      });
    })
    .catch((error) => {
      console.error(error);
      EventBus.$emit("error", error);
    });
};

export const loadPartners = async ({ commit }) => {
  axios
    .get(`${apiBaseUrl}/partners`)
    .then((result) => {
      if (Object.prototype.hasOwnProperty.call(result, "data")) {
        let partners = result.data;
        partners = partners.map((partner) => {
          partner.profile = getPartnerProfile(partner);
          return partner;
        });

        commit({
          type: "loadPartnersFinished",
          partners: partners,
        });
      }
    })
    .catch((error) => {
      console.error(error);
      EventBus.$emit("error", error);
    });
};

export const loadDocuments = async ({ commit }) => {
  axios
    .get(`${apiBaseUrl}/wallet/document`)
    .then((result: AxiosResponse<Page<MyDocumentAPI[]>>) => {
      commit({
        type: "loadDocumentsFinished",
        documents: result.data.content,
      });
    })
    .catch((error) => {
      console.error(error);
      EventBus.$emit("error", error);
    });
};

export const loadCredentials = async ({ commit }) => {
  axios
    .get(`${apiBaseUrl}/wallet/credential`)
    .then((result: AxiosResponse<Page<AriesCredential[]>>) => {
      const credentials: AriesCredential[] = [];
      for (const credentialReference of result.data.content) {
        axios
          .get(`${apiBaseUrl}/wallet/credential/${credentialReference.id}`)
          .then((result: AxiosResponse<AriesCredential>) => {
            credentials.push(result.data);
          })
          .catch((error) => {
            console.error(error);
            EventBus.$emit("error", error);
          });
      }
      commit({
        type: "loadCredentialsFinished",
        credentials: credentials,
      });
    })
    .catch((error) => {
      console.error(error);
      EventBus.$emit("error", error);
    });
};

export const loadSettings = async ({ commit }) => {
  axios
    .get(`${apiBaseUrl}/admin/config`)
    .then((result) => {
      if (Object.prototype.hasOwnProperty.call(result, "data")) {
        const settings = result.data;
        commit({
          type: "setSettings",
          settings: settings,
        });
      }
    })
    .catch((error) => {
      console.error(error);
      EventBus.$emit("error", error);
    });
};

export const loadProofTemplates = async ({ commit }) => {
  proofTemplateService
    .getProofTemplates()
    .then((result) => {
      const proofTemplates: ProofTemplate[] = result.data;

      commit({
        type: "setProofTemplates",
        proofTemplates: proofTemplates,
      });
    })
    .catch((error) => {
      console.error(error);
      EventBus.$emit("error", error);
    });
};

export const loadPartnerSelectList = async ({ commit }) => {
  partnerService
    .listPartners()
    .then((result) => {
      if (result.status === 200) {
        // filter out partners that are only at the invitation stage, we can't do anything until they accept.
        const partners = result.data
          .filter((partner) => {
            return partner.state !== PartnerStates.INVITATION.value;
          })
          .map((p) => {
            return { value: p.id, text: p.name, ...p };
          });
        commit({
          type: "setPartnerSelectList",
          list: partners,
        });
      }
    })
    .catch((error) => {
      console.error(error);
      EventBus.$emit("error", error);
    });
};
