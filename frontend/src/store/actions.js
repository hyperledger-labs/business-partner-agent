import moment from "moment";
import { CredentialTypes, PartnerStates } from "../constants";
import { EventBus, axios, apiBaseUrl } from "../main";
import { getPartnerProfile } from "../utils/partnerUtils";
import adminService from "@/services/adminService";
import proofTemplateService from "@/services/proofTemplateService";
import partnerService from "@/services/partnerService";
import issuerService from "@/services/issuerService";
import * as textUtils from "@/utils/textUtils";

export const loadSchemas = async ({ commit }) => {
  adminService
    .listSchemas()
    .then((result) => {
      let schemas = result.data;
      schemas.map((schema) => {
        if ({}.hasOwnProperty.call(schema, "schemaId")) {
          schema.type = CredentialTypes.INDY.type;
        } else if (
          !{}.hasOwnProperty.call(schema, "schemaId") &&
          !{}.hasOwnProperty.call(schema, "type")
        ) {
          schema.type = CredentialTypes.UNKNOWN.type;
        }
        schema.canIssue =
          Array.isArray(schema.credentialDefinitions) &&
          schema.credentialDefinitions.length;
      });
      schemas.unshift(CredentialTypes.PROFILE);
      commit({
        type: "setSchemas",
        schemas: schemas,
      });
    })
    .catch((e) => {
      console.error(e);
      EventBus.$emit("error", e);
    });
};

export const loadTags = async ({ commit }) => {
  adminService
    .listTags()
    .then((result) => {
      let tags = result.data;
      console.log(tags);
      commit({
        type: "setTags",
        tags: tags,
      });
    })
    .catch((e) => {
      console.error(e);
      EventBus.$emit("error", e);
    });
};

export const loadPartners = async ({ commit }) => {
  axios
    .get(`${apiBaseUrl}/partners`)
    .then((result) => {
      if ({}.hasOwnProperty.call(result, "data")) {
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
    .catch((e) => {
      console.error(e);
      EventBus.$emit("error", e);
    });
};

export const loadDocuments = async ({ commit }) => {
  axios
    .get(`${apiBaseUrl}/wallet/document`)
    .then((result) => {
      if ({}.hasOwnProperty.call(result, "data")) {
        var documents = result.data;
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
    .catch((e) => {
      console.error(e);
      EventBus.$emit("error", e);
    });
};

export const loadCredentials = async ({ commit }) => {
  axios
    .get(`${apiBaseUrl}/wallet/credential`)
    .then((result) => {
      if ({}.hasOwnProperty.call(result, "data")) {
        var credentials = [];
        result.data.forEach((credentialRef) => {
          axios
            .get(`${apiBaseUrl}/wallet/credential/${credentialRef.id}`)
            .then((result) => {
              credentials.push(result.data);
            })
            .catch((e) => {
              console.error(e);
              EventBus.$emit("error", e);
            });
        });
        commit({
          type: "loadCredentialsFinished",
          credentials: credentials,
        });
      }
    })
    .catch((e) => {
      console.error(e);
      EventBus.$emit("error", e);
    });
};

export const loadSettings = async ({ commit }) => {
  axios
    .get(`${apiBaseUrl}/admin/config`)
    .then((result) => {
      if ({}.hasOwnProperty.call(result, "data")) {
        let settings = result.data;
        commit({
          type: "setSettings",
          settings: settings,
        });
      }
    })
    .catch((e) => {
      console.error(e);
      EventBus.$emit("error", e);
    });
};

export const loadProofTemplates = async ({ commit }) => {
  proofTemplateService
    .getProofTemplates()
    .then((result) => {
      let proofTemplates = result.data;

      commit({
        type: "setProofTemplates",
        proofTemplates: proofTemplates,
      });
    })
    .catch((e) => {
      console.error(e);
      EventBus.$emit("error", e);
    });
};

export const loadPartnerSelectList = async ({ commit }) => {
  partnerService
    .listPartners()
    .then((result) => {
      if (result.status === 200) {
        // filter out partners that are only at the invitation stage, we can't do anything until they accept.
        let partners = result.data
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
    .catch((e) => {
      console.error(e);
      EventBus.$emit("error", e);
    });
};

export const loadCredDefSelectList = async ({ commit }) => {
  issuerService
    .listCredDefs()
    .then((result) => {
      if (result.status === 200) {
        let credDefs = result.data.map((c) => {
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
    .catch((e) => {
      console.error(e);
      EventBus.$emit("error", e);
    });
};
