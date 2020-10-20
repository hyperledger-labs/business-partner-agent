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
              <v-list-item-title>Business Partner Agent</v-list-item-title>
              <!-- <v-list-item-subtitle></v-list-item-subtitle> -->
            </v-list-item-content>
          </v-list-item>
        </router-link>
        <v-list-item v-if="expertMode" link :to="{ name: 'Identity' }">
          <v-list-item-action>
            <v-icon>mdi-fingerprint</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>Identity</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link :to="{ name: 'Dashboard' }" exact>
          <v-list-item-action>
            <v-icon>mdi-view-dashboard</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>Dashboard</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link :to="{ name: 'PublicProfile' }">
          <v-list-item-action>
            <v-icon>mdi-globe-model</v-icon>
          </v-list-item-action>
          <v-list-item-title>Public Profile</v-list-item-title>
        </v-list-item>
        <v-list-item link :to="{ name: 'Wallet' }">
          <v-list-item-action>
            <v-icon>mdi-cards-outline</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>Wallet</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link :to="{ name: 'Partners' }">
          <v-list-item-action>
            <v-icon>mdi-handshake</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>Business Partners</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link :to="{ name: 'Settings' }">
          <v-list-item-action>
            <v-icon>mdi-cog-outline</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>Settings</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
      </v-list>
    </v-navigation-drawer>

    <v-app-bar color="primary" app flat dark>
      <v-app-bar-nav-icon @click.stop="drawer = !drawer" />
      <v-toolbar-title>{{ title }}</v-toolbar-title>

      <v-spacer></v-spacer>

      <v-btn icon @click="logout()">
        <v-icon>mdi-logout</v-icon>
      </v-btn>
    </v-app-bar>

    <v-main>
      <router-view></router-view>
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

    <v-footer app>
      <span></span>
    </v-footer>
  </v-app>
</template>

<script>
import { EventBus } from "./main";
export default {
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
  },

  created() {
    this.$vuetify.theme.dark = false;

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
      this.$axios
        .post(`${this.$apiBaseUrl}/logout`)
        .then(() => {
          location.reload();
        })
        .catch((e) => {
          console.error(e);
          location.reload();
        });
    },
  },
};
</script>
<style>
.bg-light {
  background-color: #fafafa;
}
</style>
