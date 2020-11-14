<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <div v-if="publicDocumentsAndCredentials.length > 0">
      <v-alert colored-border color="primary" border="left" elevation="2" dense>
        <span class="text-caption"
          >You can change the visibility settings of documents and verified
          credentials in the <strong>wallet</strong> to update your public
          profile.
        </span>
      </v-alert>
      <v-card class="mx-auto" flat>
        <Profile
          v-bind:partner="{ credential: publicDocumentsAndCredentials }"
        />
      </v-card>
    </div>

    <v-container v-else fill-height fluid text-center>
      <v-row align="center" justify="center">
        <v-col>
          <h1 class="grey--text text--lighten-2">
            You don't have set up a public profile yet
          </h1>
          <v-btn color="primary" :to="{ name: 'Wallet' }" text
            >Setup your Profile</v-btn
          >
        </v-col>
      </v-row>
    </v-container>
  </v-container>
</template>

<script>
import Profile from "@/components/Profile";
import { EventBus } from "../main";
export default {
  name: "PublicProfile",
  props: {},
  components: {
    Profile,
  },
  computed: {
    document() {
      return this.$store.getters.organizationalProfile;
    },
    isBusy() {
      return this.$store.getters.isBusy;
    },
    publicDocumentsAndCredentials() {
      return this.$store.getters.publicDocumentsAndCredentials;
    },
  },
  created() {
    EventBus.$emit("title", "Public Profile");
    this.$store.dispatch("loadDocuments");
    this.$store.dispatch("loadCredentials");
  },
};
</script>
