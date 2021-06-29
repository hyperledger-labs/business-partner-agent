<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <div>
    <br />
    <v-row v-for="(value, key) in proofRequest.requestedAttributes" :key="key">
      <v-col cols="4">
        <v-row v-for="name in value.names" :key="name">
          {{ name }}
        </v-row>
      </v-col>
      <v-col cols="4">
        <v-row v-for="(value, restrict) in value.restrictions[0]" :key="value">
          {{ restrict }}:{{ value }}
        </v-row>
      </v-col>
    </v-row>
    <br />
  </div>
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
