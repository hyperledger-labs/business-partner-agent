<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container justify-center>
    <v-card class="mx-auto">
      <!-- Title -->
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        <span>Proof Templates</span>
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
                Create Proof Template
              </v-bpa-button>
            </template>
          </v-dialog>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import ProofTemplatesList from "@/components/proof-templates/ProofTemplatesList";
import VBpaButton from "@/components/BpaButton";
import store from "@/store";

export default {
  name: "ProofTemplates",
  components: { ProofTemplatesList, VBpaButton },
  created() {
    EventBus.$emit("title", "Proof Templates");
  },
  data: () => {
    return {
      proofTemplateCreateDialog: false,
    };
  },
  computed: {},
  watch: {},
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
