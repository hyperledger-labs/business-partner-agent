<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        {{ title }}
      </v-card-title>
      <v-card-text>
        <Cred v-bind:document="document" isReadOnly showOnlyContent></Cred>
        <v-divider></v-divider>
      </v-card-text>
      <v-card-actions>
        <v-expansion-panels v-if="expertMode" accordion flat>
          <v-expansion-panel>
            <v-expansion-panel-header
              class="grey--text text--darken-2 font-weight-medium bg-light"
              >Show raw data</v-expansion-panel-header
            >
            <v-expansion-panel-content class="bg-light">
              <vue-json-pretty :data="credential"></vue-json-pretty>
            </v-expansion-panel-content>
          </v-expansion-panel>
        </v-expansion-panels>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "../main";

import Cred from "@/components/Credential";
import { CredentialTypes } from "../constants";

export default {
  name: "ViewCredentialContent",
  props: {
    credential: Object,
    title: String,
  },
  created() {
    EventBus.$emit("title", "View Credential Content");
    // transform this to work with the Credential component
    this.document = {
      credentialData: { ...this.credential.attrs },
      schemaId: this.credential.schemaId,
      credentialDefinitionId: this.credential.credentialDefinitionId,
    };
  },
  data: () => {
    return {
      isBusy: false,
      isReady: false,
      CredentialTypes: CredentialTypes,
      document: {},
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
  },
  methods: {},
  components: {
    Cred,
  },
};
</script>

<style scoped>
.bg-light {
  background-color: #fafafa;
}

.bg-light-2 {
  background-color: #ececec;
}
</style>
