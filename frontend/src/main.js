/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
*/

import Vue from "vue";
import axios from "axios";
import App from "./App.vue";
import vuetify from "./plugins/vuetify";
import "@babel/polyfill";
import router from "./router";
import store from "./store";
import { CredentialTypes } from "./constants";

Vue.use(require("vue-moment"));

var apiBaseUrl;
if (process.env.NODE_ENV === "development") {
  apiBaseUrl = "http://localhost:8080/api";
  store.commit({
    type: "setSettings",
    isExpert: true,
  });
} else {
  apiBaseUrl = "/api";
}

Vue.prototype.$axios = axios;
Vue.prototype.$apiBaseUrl = apiBaseUrl;
Vue.config.productionTip = false;

Vue.filter("credentialLabel", function (name) {
  if (!name) return "";
  let itemOfName = Object.values(CredentialTypes).find((item) => {
    return item.name === name;
  });
  return itemOfName.label;
});

Vue.filter("credentialTag", function (credDefId) {
  if (!credDefId) return "";
  let pos = credDefId.lastIndexOf(":");
  return credDefId.substring(pos + 1);
});

Vue.filter('capitalize', function (string) {
  return string.replace(/\w\S*/g, (w) => (w.replace(/^\w/, (c) => c.toUpperCase())));
});


const EventBus = new Vue();
export { EventBus, axios, apiBaseUrl };

console.log(Vue.prototype);

new Vue({
  vuetify,
  router,
  store,
  render: (h) => h(App),
}).$mount("#app");
