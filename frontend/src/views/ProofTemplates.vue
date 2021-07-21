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
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import ProofTemplatesList from "@/components/ProofTemplatesList";

export default {
  name: "ProofTemplates",
  components: { ProofTemplatesList },
  created() {
    EventBus.$emit("title", "Proof Templates");
  },
  data: () => {
    return {};
  },
  computed: {},
  watch: {},
  methods: {},
};
</script>
