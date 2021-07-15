<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container> </v-container>
</template>

<script>
import { EventBus } from "@/main";

export default {
  name: "ProofTemplates",
  components: {},
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
