<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-app>
    <v-navigation-drawer v-model="drawer" app>
      <v-list dense>
        <router-link tag="span" :to="{ name: 'Dashboard' }">
          <v-list-item v-if="logo" two-line class="pl-3 mt-n2">
            <v-list-item-content>
              <v-list-item-title
                ><v-img v-if="logo" :src="logo"></v-img
              ></v-list-item-title>
              <v-list-item-subtitle
                >Business Partner Agent</v-list-item-subtitle
              >
            </v-list-item-content>
          </v-list-item>
          <v-list-item v-else two-line class="pl-3 mt-n2">
            <v-list-item-avatar>
              <v-img v-if="logo" :src="logo"></v-img>
              <!-- Default logo from https://logodust.com/ -->
              <v-img src="@/assets/logo_default.svg"></v-img>
            </v-list-item-avatar>
            <v-list-item-content>
              <v-list-item-title>{{ getAgentName }}</v-list-item-title>
              <!-- <v-list-item-subtitle></v-list-item-subtitle> -->
            </v-list-item-content>
          </v-list-item>
        </router-link>
        <v-list-item v-if="expertMode" link :to="{ name: 'Identity' }">
          <v-list-item-action>
            <v-icon>$vuetify.icons.identity</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>Identity</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link :to="{ name: 'Dashboard' }" exact>
          <v-list-item-action>
            <v-icon>$vuetify.icons.dashboard</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>Dashboard</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link :to="{ name: 'PublicProfile' }">
          <v-list-item-action>
            <v-icon>$vuetify.icons.profile</v-icon>
          </v-list-item-action>
          <v-list-item-title>Public Profile</v-list-item-title>
        </v-list-item>
        <v-list-item link :to="{ name: 'Wallet' }">
          <v-list-item-action>
            <v-badge
              overlap
              bordered
              :content="newCredentialsCount"
              :value="newCredentialsCount"
              color="red"
              offset-x="10"
              offset-y="10"
            >
              <v-icon>$vuetify.icons.wallet</v-icon>
            </v-badge>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>Wallet</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link :to="{ name: 'CredentialManagement' }">
          <v-list-item-action>
            <v-icon>$vuetify.icons.credentialManagement</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>Credential Management</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link :to="{ name: 'Partners' }">
          <v-list-item-action>
            <v-badge
              overlap
              bordered
              :content="newPartnerEventsCount"
              :value="newPartnerEventsCount"
              color="red"
              offset-x="10"
              offset-y="10"
            >
              <v-icon>$vuetify.icons.partners</v-icon>
            </v-badge>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>Business Partners</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link :to="{ name: 'Settings' }">
          <v-list-item-action>
            <v-icon>$vuetify.icons.settings</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>Settings</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
      </v-list>
      <template v-slot:append>
        <v-list dense>
          <v-list-item bottom link :to="{ name: 'About' }">
            <v-list-item-action>
              <v-icon>$vuetify.icons.about</v-icon>
            </v-list-item-action>
            <v-list-item-content>
              <v-list-item-title>About</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </v-list>
      </template>
    </v-navigation-drawer>

    <v-app-bar color="primary" app flat dark>
      <v-app-bar-nav-icon @click.stop="drawer = !drawer" />
      <v-toolbar-title>{{ title }}</v-toolbar-title>

      <v-spacer></v-spacer>

      <v-btn icon @click="logout()">
        <v-icon>$vuetify.icons.signout</v-icon>
      </v-btn>
    </v-app-bar>

    <v-main>
      <app-taa v-if="!sessionDialog && $store.getters.taaRequired"></app-taa>
      <router-view
        v-if="!sessionDialog && !$store.getters.taaRequired"
      ></router-view>
    </v-main>

    <v-snackbar
      v-model="snackbar"
      :bottom="true"
      :color="color"
      :multi-line="false"
      :right="true"
      :timeout="5000"
      :top="false"
      :vertical="true"
    >
      {{ snackbarMsg }}
      <v-btn dark text @click="snackbar = false">Close</v-btn>
    </v-snackbar>

    <v-dialog v-model="sessionDialog" max-width="290">
      <v-card>
        <v-card-title class="headline">Session expired</v-card-title>

        <v-card-text
          >It seems your session is expired. Do you want log in
          again?</v-card-text
        >

        <v-card-actions>
          <v-spacer></v-spacer>

          <v-btn color="warning" text @click="sessionDialog = false">No</v-btn>

          <v-btn color="green darken-1" text @click="logout()">Yes</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-footer v-if="showFooter" app>
      <v-col cols="12" class="text-center">
        <span v-if="imprintUrl" class="mr-4 subtitle-2"
          ><a :href="imprintUrl">Corporate Information</a></span
        >
        <span v-if="privacyPolicyUrl" class="subtitle-2"
          ><a :href="privacyPolicyUrl">Privacy Policy</a></span
        >
      </v-col>
    </v-footer>
  </v-app>
</template>

<script>
import { EventBus } from "./main";
import Taa from "./components/taa/TransactionAuthorAgreement";

export default {
  components: {
    "app-taa": Taa,
  },
  props: {
    source: String,
  },
  data: () => ({
    title: "",
    drawer: null,
    logo: process.env.VUE_APP_LOGO_URL,

    // snackbar stuff
    snackbar: false,
    color: "",
    snackbarMsg: "",

    sessionDialog: false,
  }),
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
    showFooter() {
      if (
        (this.$store.state.settings.imprint &&
          typeof this.$store.state.settings.imprint === "string") ||
        (this.$store.state.settings.dataPrivacyPolicy &&
          typeof this.$store.state.settings.dataPrivacyPolicy === "string")
      ) {
        return (
          this.$store.state.settings.imprint.length +
            this.$store.state.settings.dataPrivacyPolicy.length >
          0
        );
      } else {
        return null;
      }
    },
    imprintUrl() {
      return this.$store.state.settings.imprint;
    },
    privacyPolicyUrl() {
      return this.$store.state.settings.dataPrivacyPolicy;
    },
    newPartnerEventsCount() {
      console.log("read newPartnerEventsCount");
      console.log(this.$store.getters.newPartnerEventsCount);
      return this.$store.getters.newPartnerEventsCount;
    },
    newCredentialsCount() {
      return this.$store.getters.newCredentialsCount;
    },
    getAgentName() {
      let bpaName = "Business Partner Agent";
      const nameSettingValue = this.$store.getters.getSettingByKey("agentName");
      if (nameSettingValue) {
        bpaName = nameSettingValue;
      }
      return bpaName;
    },
  },
  created() {
    this.$vuetify.theme.dark = false;
    this.$store.dispatch("validateTaa");

    // Global Error handling
    // Todo: Put in extra component

    EventBus.$on("title", (title) => {
      this.title = title;
    });

    EventBus.$on("success", (msg) => {
      (this.snackbarMsg = msg), (this.color = "green"), (this.snackbar = true);
    });

    EventBus.$on("error", (msg) => {
      console.log(msg.response);

      if (
        {}.hasOwnProperty.call(msg, "response") &&
        {}.hasOwnProperty.call(msg.response, "status")
      ) {
        switch (msg.response.status) {
          case 401:
            this.sessionDialog = true;
        }

        if (
          {}.hasOwnProperty.call(msg.response, "data") &&
          {}.hasOwnProperty.call(msg.response.data, "message")
        ) {
          msg = msg.response.data.message;
        }
      }

      (this.snackbarMsg = msg), (this.color = "red"), (this.snackbar = true);
    });
  },
  methods: {
    logout() {
      // logout must have get-allowed, get the browser to do all the logout redirects...
      location.href = `${this.$apiBaseUrl}/logout`;
    },
  },
};
</script>
<style>
.bg-light {
  background-color: #fafafa;
}
a {
  text-decoration: none;
}
</style>
