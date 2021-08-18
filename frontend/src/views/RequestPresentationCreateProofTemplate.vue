<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <div>
    <proof-template-create></proof-template-create>
    <v-card-actions>
      <v-layout align-end justify-end>
        <v-bpa-button color="primary" @click="createAndSendProofTemplate()">
          Create and send (TODO)
        </v-bpa-button>
      </v-layout>
    </v-card-actions>
  </div>
</template>

<script>
import { EventBus } from "@/main";
import VBpaButton from "@/components/BpaButton";
import ProofTemplateCreate from "@/views/ProofTemplateCreate";

export default {
  name: "RequestPresentation",
  components: {ProofTemplateCreate, VBpaButton },
  props: {
    id: String, // partner ID
  },
  mounted() {
    EventBus.$emit("title", "Request Presentation");
  },
  data: () => {
    return {};
  },
  computed: {},
  methods: {
    createAndSendProofTemplate() {
      // TODO: Inject button functionality and label into ProofTemplateCreate component
      console.log("Not implemented yet", this.id);
    },
  },
};
</script>

<style>
.bg-light {
  background-color: #fafafa;
}
</style>
