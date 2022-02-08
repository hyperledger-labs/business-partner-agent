<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container justify-center>
    <v-card class="my-4 mx-auto">
      <!-- Title -->
      <v-card-title class="bg-light">
        <span>{{ $t("view.proofTemplates.tableTitle") }}</span>
      </v-card-title>

      <!-- Proof Templates Table -->
      <v-card-text>
        <ProofTemplatesList />
      </v-card-text>

      <!-- Proof Templates Actions -->
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-dialog
            v-model="proofTemplateCreateDialog"
            persistent
            max-width="600px"
          >
            <template v-slot:activator="{ on, attrs }">
              <v-bpa-button
                v-bind="attrs"
                v-on="on"
                color="primary"
                @click="proofTemplateCreate()"
              >
                {{ $t("view.proofTemplate.createProofTemplate") }}
              </v-bpa-button>
            </template>
          </v-dialog>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import ProofTemplatesList from "@/components/proof-templates/ProofTemplatesList.vue";
import VBpaButton from "@/components/BpaButton";
import store from "@/store";

export default {
  name: "ProofTemplates",
  components: { ProofTemplatesList, VBpaButton },
  created() {
    EventBus.$emit("title", this.$t("view.proofTemplates.title"));
  },
  data: () => {
    return {
      proofTemplateCreateDialog: false,
    };
  },
  methods: {
    onProofTemplateCreated() {
      store.dispatch("loadProofTemplates");
      this.proofTemplateCreateDialog = false;
    },
    proofTemplateCreate() {
      this.$router.push({
        name: "ProofTemplateCreate",
        params: {},
      });
    },
  },
};
</script>
