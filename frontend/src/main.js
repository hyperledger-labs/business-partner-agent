/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
*/

import Vue from "vue";
import axios from "axios";
import VueNativeSock from "vue-native-websocket";
import App from "./App.vue";
import vuetify from "./plugins/vuetify";
import "@babel/polyfill";
import router from "./router";
import store from "./store";
import SortUtil from "./utils/sortUtils";
import "@/filters";

import VueJsonPretty from "vue-json-pretty";
import "vue-json-pretty/lib/styles.css";
Vue.component("vue-json-pretty", VueJsonPretty);

Vue.use(require("vue-moment"));
Vue.use(SortUtil);

var apiBaseUrl = process.env.VUE_APP_API_BASE_URL;
var eventsHost = process.env.VUE_APP_EVENTS_HOST
  ? process.env.VUE_APP_EVENTS_HOST
  : window.location.host;
var socketApi = `${
  window.location.protocol === "https:" ? "wss" : "ws"
}://${eventsHost}/${process.env.VUE_APP_EVENTS_PATH}`;

if (process.env.NODE_ENV === "development") {
  store.commit({
    type: "setExpertMode",
    isExpert: true,
  });
}

Vue.use(VueNativeSock, socketApi, {
  store: store,
  format: "json",
  reconnection: true,
  passToStoreHandler: function (eventName, event) {
    if (!eventName.startsWith("SOCKET_")) {
      return;
    }
    console.log(event);
    let msg = event;
    let method = "commit";
    let target = eventName.toUpperCase();
    if (target === "SOCKET_ONMESSAGE") {
      if (this.format === "json" && event.data) {
        msg = JSON.parse(event.data);
        // method = 'dispatch';
        switch (msg.message.type) {
          case "CONNECTION_REQUEST":
            target = "newPartner";
            break;
          case "PARTNER":
            target = "newPartner";
            break;
          case "CREDENTIAL":
            target = "newCredential";
            break;
          case "PROOF":
            target = "newPresentation";
            break;
        }
      }
    }
    this.store[method](target, msg);
  },
});

Vue.prototype.$axios = axios;
Vue.prototype.$apiBaseUrl = apiBaseUrl;
Vue.config.productionTip = false;

Vue.filter("credentialTag", function (credDefId) {
  if (!credDefId) return "";
  let pos = credDefId.lastIndexOf(":");
  return credDefId.substring(pos + 1);
});

Vue.filter("capitalize", function (string) {
  return string && string !== ""
    ? string.replace(/\w\S*/g, (w) => w.replace(/^\w/, (c) => c.toUpperCase()))
    : "";
});

// Get Configuration
Vue.prototype.$config = {
  ledger: "iil",
};
axios
  .get(`${apiBaseUrl}/admin/config`)
  .then((result) => {
    if ({}.hasOwnProperty.call(result, "data")) {
      Vue.prototype.$config = result.data;
      let ledgerPrefix = Vue.prototype.$config.ledgerPrefix;
      let splitted = ledgerPrefix.split(":");
      Vue.prototype.$config.ledger = splitted[splitted.length - 2];
    }
  })
  .catch((e) => {
    console.error(e);
  });

const EventBus = new Vue();
export { EventBus, axios, apiBaseUrl };

console.log(Vue.prototype);
store.dispatch("loadSettings");
store.dispatch("loadSchemas");

new Vue({
  vuetify,
  router,
  store,
  render: (h) => h(App),
}).$mount("#app");
