<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <br />
    <v-row v-for="(value, key) in proofRequest.requestedAttributes" :key="key">
      <v-col cols="4" class="pb-0">
        <v-row>
          <p class="grey--text text--darken-2 font-weight-medium">
            Requested Fields
          </p>
        </v-row>
        <v-row v-for="name in value.names" :key="name" style="height: 30px">
          - {{ name }}
        </v-row>
      </v-col>
      <v-col cols="6" class="pb-0">
        <v-row>
          <p class="grey--text text--darken-2 font-weight-medium">
            Restrictions
          </p>
        </v-row>
        <v-row
          v-for="(value, restrict) in value.restrictions[0]"
          :key="value"
          style="height: 30px"
        >
          {{ restrict }} = {{ value }}
        </v-row>
      </v-col>
    </v-row>
    <br />
  </v-container>
</template>

<script>
export default {
  name: "ProofRequest",
  props: {
    proofRequest: Object,
  },
  mounted() {
    console.log("proof_request");
    console.log(this.proofRequest);
  },
  data: () => {
    return {};
  },
};
</script>
