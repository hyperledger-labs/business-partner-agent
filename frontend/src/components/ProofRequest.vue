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
        <v-row v-for="(value, restrict) in value.restrictions[0]" :key="value">
          <v-expansion-panels accordion flat>
            <v-expansion-panel>
              <v-expansion-panel-header
                class="grey--text text--darken-2 font-weight-medium bg-light"
              >
                {{ restrict }} = {{ value }}</v-expansion-panel-header
              >
              <v-expansion-panel-content class="bg-light">
                <Cred
                  v-if="restrict === 'schema_id'"
                  :document="findCredentialby(restrict, value)"
                  class="right"
                  isReadOnly
                  showOnlyContent
                ></Cred>
              </v-expansion-panel-content>
            </v-expansion-panel>
          </v-expansion-panels>
        </v-row>
      </v-col>
    </v-row>
    <br />
  </v-container>
</template>

<script>
import Cred from "@/components/Credential.vue";

export default {
  name: "ProofRequest",
  props: {
    proofRequest: Object,
    credentials: [],
  },
  mounted() {},
  methods: {
    findCredentialby(field, value) {
      let creds = this.credentials.filter((cred) => {
        return cred[this.field_map[field]] === value;
      });
      console.log("CREDs   " + creds);
      return creds[0];
    },
  },
  data: () => {
    return {
      credentials: [],
      valid_credentials: {},
      field_map: { schema_id: "schemaId" },
    };
  },
  components: {
    Cred,
  },
};
</script>

<style scoped>
.v-btn {
  margin-left: 10px;
}
</style>
