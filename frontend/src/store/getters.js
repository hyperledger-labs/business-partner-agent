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
  return schemaType.label;
};

export const createTemplateFromSchemaId = (state) => (schemaId) => {
  let schema = state.schemas.find((schema) => {
    return schema.schemaId === schemaId;
  });
  const objectTemplate = Object.assign(schema, {
    fields: schema.schemaAttributeNames.map((key) => {
      return {
        type: key,
        label: key
          ? key.substring(0, 1).toUpperCase() +
            key.substring(1).replace(/([a-z])([A-Z])/g, "$1 $2")
          : "",
      };
    }),
  });
  console.log("OBJECT TEMPLATE", objectTemplate, schemaId);
  return objectTemplate;
};

/*schemas: (state) => {
    return state.schemas;
  },
  getSchema: (state) => (schemaId) => {
    if (!schemaId) {
      return null;
    }
    return state.schemas.find((schema) => {
      return schema.schemaId === schemaId;
    });
  },
  getPreparedSchema: (state) => (schemaId) => {
    let schema = state.schemas.find((schema) => {
      return schema.schemaId === schemaId;
    });
    console.log(schema);
    return Object.assign(schema, {
      fields: schema.schemaAttributeNames.map((key) => {
        return {
          type: key,
          label: key,
        };
      }),
    });
  },*/

export const getSettingByKey = (state) => (key) => {
  if (state.settings && {}.hasOwnProperty.call(state.settings, key)) {
    return state.settings[key];
  }
};

export const getSettings = (state) => {
  return state.settings;
};
