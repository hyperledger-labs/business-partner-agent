<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-card class="my-2 rounded-lg align-self-start" width="400" elevation="5">
    <v-card-title
      height="40"
      class="light-blue darken-3 card-title text-subtitle-1 white--text"
      style="text-transform: capitalize"
      >{{ this.document.proofData.identifier.schemaLabel }}</v-card-title
    >
    <v-card-text>
      <v-container
        v-for="[key, value] in Object.entries(
          this.document.proofData.revealedAttributes
        )"
        v-bind:key="key"
      >
        <v-row>
          <v-col class="col-12">
            <p class="font-weight-medium">{{ key }}</p>
            <p class="font-italic">{{ value }}</p>
          </v-col>
        </v-row>
      </v-container>
      <v-expansion-panels flat>
        <v-expansion-panel>
          <v-expansion-panel-header class="font-weight-medium">
            {{ $t("component.credentialCard.details") }}
            <template v-slot:actions>
              <v-icon color="primary"> $expand </v-icon>
            </template>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <v-list dense>
              <v-list-item
                v-for="[key, value] in Object.entries(
                  this.document.proofData.identifier
                )"
                v-bind:key="key"
              >
                <v-list-item-content>
                  <v-list-item-title class="font-weight-medium">
                    {{ key }}
                  </v-list-item-title>
                  <v-list-item-subtitle align="">
                    {{ value }}
                  </v-list-item-subtitle>
                </v-list-item-content>
              </v-list-item>
            </v-list>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
    </v-card-text>
  </v-card>
</template>

<script lang="ts">
export default {
  props: {
    document: Object,
    isNew: Boolean,
  },
  data: () => {
    return {
      intDoc: {
        type: Object,
        default: {},
      },
      origIntDoc: Object,
    };
  },
};
</script>

<style scoped>
.v-application p {
  margin-bottom: 4px;
}
</style>
