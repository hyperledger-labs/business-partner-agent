import { appAxios } from "@/services/interceptors";
import { ApiRoutes } from "@/constants";

export default {
  //
  // Partner API
  //

  listPartners() {
    return appAxios().get(`${ApiRoutes.PARTNERS}`);
  }
};
