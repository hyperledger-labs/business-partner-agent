<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <proof-template-create
    disable-route-back
    create-button-label="Create and Send"
    v-on:received-proof-template-id="submitRequest($event)"
  ></proof-template-create>
</template>

<script>
import { EventBus } from "@/main";
import ProofTemplateCreate from "@/views/ProofTemplateCreate";
import proofTemplateService from "@/services/proofTemplateService";

export default {
  name: "RequestPresentation",
  components: { ProofTemplateCreate },
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
    async submitRequest(proofTemplateId) {
      proofTemplateService
        .sendProofTemplate(proofTemplateId, this.id)
        .then(() => {
          EventBus.$emit("success", "Presentation request sent");
          this.$router.go(-2);
        })
        .catch((e) => {
          EventBus.$emit("error", this.$axiosErrorMessage(e));
        });
    },
  },
};
</script>

<style>
.bg-light {
  background-color: #fafafa;
}
</style>
