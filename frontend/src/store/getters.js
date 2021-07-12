import { CredentialTypes } from "../constants";

export const istBusy = (state) => {
  return state.busyStack > 0;
};

export const publicDocumentsAndCredentials = (state) => {
  var retval = state.credentials
    .concat(state.documents)
    .filter((d) => d.isPublic == true);
  return retval;
};

export const getOrganizationalProfile = (state) => {
  var documents = state.documents.filter(
    (d) => d.type == CredentialTypes.PROFILE.type
  );
  if (documents.length == 1) return documents[0];
  else return undefined;
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
    return schema.type === CredentialTypes.SCHEMA_BASED.type;
  });
};

export const getSchemaById = (state) => (schemaId) => {
  if (!schemaId) {
    return null;
  }
  return state.schemas.find((schema) => {
    return schema.schemaId === schemaId;
  });
};

export const getSchemaByType = (state) => (schemaType) => {
  if (!schemaType) {
    return null;
  }
  return state.schemas.find((schema) => {
    return schema.type === schemaType;
  });
};

export const getSchemaLabel = (state) => (typeName, schemaId = undefined) => {
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

  if (schemaType && {}.hasOwnProperty.call(schemaType, "label")) {
    return schemaType.label;
  }

  if (schemaId) {
    //Maybe in backend get label for schemaId
    return schemaId;
  }
  return "";
};

export const getSettingByKey = (state) => (key) => {
  if (state.settings && {}.hasOwnProperty.call(state.settings, key)) {
    return state.settings[key];
  }
};

export const getSettings = (state) => {
  return state.settings;
};
