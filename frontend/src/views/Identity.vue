<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container justify-center>
    <v-card class="mx-auto" max-width="400" flat>
      <v-card-title class="grey--text text--darken-2">
        Identity Information
      </v-card-title>

      <vue-json-pretty v-if="didDocLoaded" :data="didDoc"> </vue-json-pretty>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
export default {
  name: "Identity",
<<<<<<< HEAD
=======
  components: {
    VueJsonPretty,
  },
>>>>>>> bpa/master
  created() {
    EventBus.$emit("title", "Identity");
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
        .catch((e) => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
  },
};
</script>
