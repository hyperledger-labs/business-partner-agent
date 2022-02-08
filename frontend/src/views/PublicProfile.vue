<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <div v-if="publicDocumentsAndCredentials.length > 0">
      <v-alert colored-border color="primary" border="left" elevation="2" dense>
        <span class="text-caption" v-html="$t('view.profile.subtitle')"></span>
      </v-alert>
      <v-card class="mx-auto" flat>
        <Profile
          v-bind:partner="{ credential: publicDocumentsAndCredentials }"
          organization-profile-edit-visible
        />
      </v-card>
    </div>

    <v-container v-else fill-height fluid text-center>
      <v-row align="center" justify="center">
        <v-col>
          <h1 class="grey--text text--lighten-2">
            <span v-html="$t('view.profile.noProfileTitle')" />
          </h1>
          <span
            class="grey--text text--lighten"
            v-html="$t('view.profile.noProfileSubTitle')"
          /><br />
          <v-btn color="primary" :to="{ name: 'Wallet' }" text>{{
            $t("view.profile.buttonNoProfile")
          }}</v-btn>
        </v-col>
      </v-row>
    </v-container>
  </v-container>
</template>

<script lang="ts">
import Profile from "@/components/Profile.vue";
import { EventBus } from "@/main";
export default {
  name: "PublicProfile",
  props: {},
  components: {
    Profile,
  },
  computed: {
    isBusy() {
      return this.$store.getters.isBusy;
    },
    publicDocumentsAndCredentials() {
      return this.$store.getters.publicDocumentsAndCredentials;
    },
  },
  created() {
    EventBus.$emit("title", this.$t("view.profile.title"));

    this.$store.dispatch("loadDocuments");
    this.$store.dispatch("loadCredentials");
  },
};
</script>
