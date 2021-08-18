<!--
 Copyright (c) 2021 - for information on the respective copyright owner
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
        Create a Presentation Request
      </v-card-title>
      <v-card-text>
        <proof-templates-list
            show-checkboxes
            v-model="selectedProofTemplate"
        >
        </proof-templates-list>
        <template>
          <v-card-actions>
            <v-layout align-end justify-end>
              <v-bpa-button color="secondary" @click="cancel()"
              >Cancel</v-bpa-button
              >
              <v-bpa-button color="primary" @click="openCreateProofTemplate()">
                Create Proof Template
              </v-bpa-button>
              <v-bpa-button
                  :loading="this.isBusy"
                  :disabled="selectedProofTemplate.length === 0"
                  color="primary"
                  @click="submitRequest()"
              >
                Send Request
              </v-bpa-button>
            </v-layout>
          </v-card-actions>
        </template>
      </v-card-text>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import VBpaButton from "@/components/BpaButton";
import ProofTemplatesList from "@/components/proof-templates/ProofTemplatesList";
import proofTemplateService from "@/services/proofTemplateService";

export default {
  name: "RequestPresentation",
  components: {ProofTemplatesList, VBpaButton },
  props: {
    id: String, // partner ID
  },
  mounted() {
    EventBus.$emit("title", "Request Presentation");
  },
  data: () => {
    return {
      isBusy: false,
      selectedProofTemplate: [],
      selectedSchema: [],
      selectedIssuer: [],
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
  },
  methods: {
    openCreateProofTemplate() {
      this.$router.push({
        name: "RequestPresentationCreateProofTemplate",
        params: {
          id: this.id,
        },
      });
    },
    async submitRequest() {
      this.isBusy = true;

      proofTemplateService.sendProofTemplate(this.selectedProofTemplate[0].id, this.id).then(() => {
        EventBus.$emit("success", "Presentation request sent");
        this.$router.go(-1);
      }).catch((e) => {
        console.error(e);
        EventBus.$emit("error", e);
      });

      this.isBusy = false;
    },
    cancel() {
      this.$router.go(-1);
    },
  },
};
</script>

<style>
.bg-light {
  background-color: #fafafa;
}
</style>
