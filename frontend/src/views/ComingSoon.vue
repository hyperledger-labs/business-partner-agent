<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <p>{{ $t("view.comingSoon.text") }}</p>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
export default {
  name: "ComingSoon",
  components: {},
  created() {
    EventBus.$emit("title", this.$t("view.comingSoon.title"));
  },
  data: () => {
    return {};
  },
};
</script>
