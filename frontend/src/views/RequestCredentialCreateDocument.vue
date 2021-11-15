<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <Document></Document>
</template>

<script>
import { EventBus } from "@/main";
import Document from "@/views/Document.vue";

export default {
  name: "RequestCredentialCreateDocument",
  components: { Document },
  props: {
    id: String, // partner ID
  },
  mounted() {
    EventBus.$emit("title", this.$t("view.requestPresentation.title"));
  },
};
</script>
