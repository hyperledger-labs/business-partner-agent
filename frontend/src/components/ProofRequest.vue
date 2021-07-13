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
      <v-col cols="8" class="pb-0">
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
          <v-btn
            x-small
            v-if="restrict === 'schema_id'"
            slot="badge"
            @click="navigateToCredentialby(restrict, value)"
          >
            Go To
          </v-btn>
        </v-row>
      </v-col>
    </v-row>
    <br />
  </v-container>
</template>

<script>
import { EventBus } from "../main";

export default {
  name: "ProofRequest",
  props: {
    proofRequest: Object,
    credentials: [],
  },
  mounted() {},
  methods: {
    navigateToCredentialby(field, value) {
      let creds = this.credentials.filter((cred) => {
        return cred[this.field_map[field]] === value;
      });
      console.log("CRED" + creds[0].id);
      let cred = creds[0];
      if (cred) {
        this.$router.push({
          name: "Credential",
          params: {
            id: cred.id,
          },
        });
      } else {
        EventBus.$emit("error", "No credential that has this schema_id");
      }
    },
  },
  data: () => {
    return {
      credentials: [],
      valid_credential: {},
      field_map: { schema_id: "schemaId" },
    };
  },
};
</script>

<style scoped>
.v-btn {
  margin-left: 10px;
}
</style>
