<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container justify-center>
    <v-card class="mx-auto" max-width="400" flat>
      <v-card-title class="grey--text text--darken-2">
        {{ $t("view.identity.titleInfo") }}
      </v-card-title>

      <vue-json-pretty v-if="didDocLoaded" :data="didDoc"> </vue-json-pretty>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
export default {
  name: "Identity",
  components: {},
  created() {
    EventBus.$emit("title", this.$t("view.identity.title"));
    this.getIdentity();
  },
  data: () => {
    return {
      didDocLoaded: false,
      didDoc: {},
    };
  },
  methods: {
    getIdentity() {
      console.log("Getting partner...");
      this.$axios
        .get(`${this.$apiBaseUrl.replace("/api", "")}/.well-known/did.json`)
        .then((result) => {
          console.log(result);
          this.didDocLoaded = true;
          this.didDoc = result.data;
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
  },
};
</script>
