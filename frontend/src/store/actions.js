import moment from "moment";
import { EventBus, axios, apiBaseUrl } from "../main";
import { getPartnerProfile } from "../utils/partnerUtils";

export const loadSchemas = async ({ commit }) => {
  axios
    .get(`${apiBaseUrl}/admin/schema`)
    .then((result) => {
      if ({}.hasOwnProperty.call(result, "data")) {
        let schemas = result.data;
        commit({
          type: "loadSchemasFinished",
          schemas: schemas,
        });
      }
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
      console.log(result);
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

// export const completeEditDocument = async ({ state }) => {
//   if (state.editedDocument.add) {
//     axios
//       .post(`${apiBaseUrl}/wallet/document`, {
//         document: state.editedDocument.document,
//         isPublic: true, //TODO
//         type: state.editedDocument.type
//       })
//       .then(() => {
//         this.dispatch('loadDocuments')
//         EventBus.$emit("success", "Success");
//       })
//       .catch((e) => {
//         console.error(e);
//         EventBus.$emit("error", e);
//       });
//   }
//   else {
//     axios
//       .put(`${apiBaseUrl}/wallet/document/${state.editedDocument.id}`, {
//         document: state.editedDocument.document,
//         isPublic: true, //TODO
//         type: state.editedDocument.type
//       })
//       .then(() => {
//         this.dispatch('loadDocuments')
//         EventBus.$emit("success", "Success");
//       })
//       .catch((e) => {
//         console.error(e);
//         EventBus.$emit("error", e);
//       });
//   }
// }
