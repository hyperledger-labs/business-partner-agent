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
        <router-link tag="span" :to="{ name: 'Home' }">
          <v-list-item two-line class="pl-3 mt-n2">
            <v-list-item-content>
              <v-list-item-title>IIL Network</v-list-item-title>
              <v-list-item-subtitle>Business Partner Agent</v-list-item-subtitle>
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
        <!-- <v-list-group > -->
        <!-- <template v-slot:activator> -->
        <v-list-item link :to="{ name: 'PublicProfile' }">
          <v-list-item-action>
            <v-icon>mdi-globe-model</v-icon>
          </v-list-item-action>
          <v-list-item-title>Public Profile</v-list-item-title>
        </v-list-item>
        <!-- </template> -->
        <!-- <v-list-item link :to="{ path: '/masterdata#base-information' }">
           <v-list-item-title>Base Information</v-list-item-title>
        </v-list-item>
        
          <v-list-item link :to="{ path: '/masterdata#registration-address' }">
             <v-list-item-title>Registration Address</v-list-item-title>
          </v-list-item>
          <v-list-item>
             <v-list-item-title>Contact Persons</v-list-item-title>
        </v-list-item>-->
        <!-- </v-list-group> -->
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

    <v-app-bar color="purple darken-4" app flat dark>
      <v-app-bar-nav-icon @click.stop="drawer = !drawer" />
      <v-toolbar-title>{{ title }}</v-toolbar-title>

      <v-spacer></v-spacer>

      <v-btn icon @click="logout()">
        <v-icon>mdi-logout</v-icon>
      </v-btn>
    </v-app-bar>

    <v-content>
      <keep-alive include="MasterData">
        <router-view></router-view>
      </keep-alive>
    </v-content>

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

        <v-card-text>It seems your session is expired. Do you want log in again?</v-card-text>

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
    source: String
  },

  data: () => ({
    title: "",
    drawer: null,

    // snackbar stuff
    snackbar: false,
    color: "",
    snackbarMsg: "",

    sessionDialog: false
  }),

  computed: {
    expertMode(){
        return this.$store.state.expertMode;
    }
  },

  created() {
    this.$vuetify.theme.dark = false;

    // Global Error handling
    // Todo: Put in extra component

    EventBus.$on("title", title => {
      this.title = title;
    });

    EventBus.$on("success", msg => {
      (this.snackbarMsg = msg), (this.color = "green"), (this.snackbar = true);
    });

    EventBus.$on("error", msg => {
      console.log(msg.response);

      if (
        {}.hasOwnProperty.call(msg, "response") &&
        {}.hasOwnProperty.call(msg.response, "status")
      ) {
        switch (msg.response.status) {
          case 401:
            this.sessionDialog = true;
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
        .catch(e => {
          console.error(e);
          location.reload();
        });
    }
  }
};
</script>
