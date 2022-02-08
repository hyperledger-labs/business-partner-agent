/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import moment from "moment";
import { CredentialTypes, PartnerStates } from "@/constants";
import { EventBus, axios, apiBaseUrl } from "../main";
import { getPartnerProfile } from "@/utils/partnerUtils";
import adminService from "@/services/adminService";
import proofTemplateService from "@/services/proofTemplateService";
import partnerService from "@/services/partnerService";
import issuerService from "@/services/issuerService";
import * as textUtils from "@/utils/textUtils";

export const loadSchemas = async ({ commit }) => {
  adminService
    .listSchemas()
    .then((result) => {
      const schemas = result.data;
      schemas.map((schema) => {
        if (Object.prototype.hasOwnProperty.call(schema, "schemaId")) {
          if (schema.type === CredentialTypes.INDY.type) {
            schema.type = CredentialTypes.INDY.type;
          } else if (schema.type === CredentialTypes.JSON_LD.type) {
            schema.type = CredentialTypes.JSON_LD.type;
          }
        } else if (
          !Object.prototype.hasOwnProperty.call(schema, "schemaId") &&
          !Object.prototype.hasOwnProperty.call(schema, "type")
        ) {
          schema.type = CredentialTypes.UNKNOWN.type;
        }
        schema.canIssue =
          Array.isArray(schema.credentialDefinitions) &&
          schema.credentialDefinitions.length > 0;
      });
      schemas.unshift(CredentialTypes.PROFILE);
      commit({
        type: "setSchemas",
        schemas: schemas,
      });
    })
    .catch((error) => {
      console.error(error);
      EventBus.$emit("error", error);
    });
};

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
    .then((result) => {
      if (Object.prototype.hasOwnProperty.call(result, "data")) {
        const documents = result.data;
        documents.map((documentIn) => {
          documentIn.createdDate = moment(documentIn.createdDate);
          documentIn.updatedDate = moment(documentIn.updatedDate);
        });

        commit({
          type: "loadDocumentsFinished",
          documents: documents,
        });
      }
    })
    .catch((error) => {
      console.error(error);
      EventBus.$emit("error", error);
    });
};

export const loadCredentials = async ({ commit }) => {
  axios
    .get(`${apiBaseUrl}/wallet/credential`)
    .then((result) => {
      if (Object.prototype.hasOwnProperty.call(result, "data")) {
        const credentials: Array<any> = [];
        for (const credentialReference of result.data) {
          axios
            .get(`${apiBaseUrl}/wallet/credential/${credentialReference.id}`)
            .then((result) => {
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
      }
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
      const proofTemplates = result.data;

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

// eslint-disable-next-line unicorn/prevent-abbreviations
export const loadCredDefSelectList = async ({ commit }) => {
  issuerService
    .listCredDefs()
    .then((result) => {
      if (result.status === 200) {
        const credDefs = result.data.map((c) => {
          return {
            value: c.id,
            text: c.displayText,
            fields: c.schema.schemaAttributeNames.map((key) => {
              return {
                type: key,
                label: textUtils.schemaAttributeLabel(key),
              };
            }),
            ...c,
          };
        });

        commit({
          type: "setCredDefSelectList",
          list: credDefs,
        });
      }
    })
    .catch((error) => {
      console.error(error);
      EventBus.$emit("error", error);
    });
};
