<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-card v-if="isReady" class="mx-auto">
    <v-card-title class="bg-light">
      <v-btn depressed color="secondary" icon @click="$router.go(-1)">
        <v-icon dark>mdi-chevron-left</v-icon>
      </v-btn>
      <div v-if="credential.type === CredentialTypes.OTHER.name" >{{ credential.credentialDefinitionId | credentialTag }}</div>
      <div v-else>{{ credential.type | credentialLabel }}</div>
      <v-layout align-end justify-end>
        <v-btn depressed color="red" icon @click="deletePresentation()">
          <v-icon dark>mdi-delete</v-icon>
        </v-btn>
      </v-layout>
    </v-card-title>
    <v-card-text>
      <Cred v-bind:document="credential" isReadOnly></Cred>
      <v-divider></v-divider>
      
    </v-card-text>
    <v-card-actions>
      <v-expansion-panels v-if="expertMode" accordion flat>
        <v-expansion-panel>
          <v-expansion-panel-header
            class="grey--text text--darken-2 font-weight-medium bg-light"
          >Show raw data</v-expansion-panel-header>
          <v-expansion-panel-content class="bg-light">
            <vue-json-pretty :data="credential"></vue-json-pretty>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
    </v-card-actions>

    <!-- <v-card-actions>
      <v-layout align-end justify-end>
        <v-btn color="secondary" text @click="cancel()">Cancel</v-btn>
        <v-btn :loading="this.isBusy" color="primary" text @click="saveChanges()">Save</v-btn>
      </v-layout>
    </v-card-actions> -->
  </v-card>
</template>

<script>
import { EventBus } from "../main";

import Cred from "@/components/Credential";
import VueJsonPretty from "vue-json-pretty";
import { CredentialTypes } from "../constants";

export default {
  name: "Presentation",
  props: {
    id: String,
    presentationId: String,
  },
  created() {
    EventBus.$emit("title", "Presentation");
    this.getPresentation();

    console.log(this.id)
    console.log(this.presentationId)
  },
  data: () => {
    return {
      document: {},
      isBusy: false,
      isReady: false,
      CredentialTypes: CredentialTypes
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    }
  },
  methods: {
    getPresentation() {
      console.log(this.id);
      this.$axios
        .get(`${this.$apiBaseUrl}/partners/${this.id}/proof/${this.presentationId}`)
        .then(result => {
          if ({}.hasOwnProperty.call(result, "data")) {
            this.credential = result.data;
            this.isPublic = this.credential.isPublic;
            this.isReady = true;
          }
        })
        .catch(e => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    // saveChanges() {
     
    // },
    deletePresentation() {
      this.$axios
        .delete(`${this.$apiBaseUrl}/partners/${this.id}/proof/${this.presentationId}`)
        .then(result => {
          console.log(result);
          if (result.status === 200) {
            EventBus.$emit("success", "Presentation deleted");
            this.$router.go(-1);
          }
        })
        .catch(e => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    cancel() {
      this.$router.go(-1)
    }
  },
  components: {
    Cred,
    VueJsonPretty
  }
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
