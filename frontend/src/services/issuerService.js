import { appAxios } from '@/services/interceptors';
import { ApiRoutes } from '@/utils/constants';

export default {
  //
  // Issuer API
  //

  listSchemas() {
    return appAxios().get(`${ApiRoutes.ISSUER}/schema`);
  },

  createSchema(data) {
    return appAxios().post(`${ApiRoutes.ISSUER}/schema`, data);
  },

  readSchema(id) {
    return appAxios().get(`${ApiRoutes.ISSUER}/schema/${id}`);
  },

  createCredDef(data) {
    return appAxios().post(`${ApiRoutes.ISSUER}/creddef`, data);
  },

  issueCredentialSend(data) {
    return appAxios().post(`${ApiRoutes.ISSUER}/issue-credential/send`, data);
  }
};
