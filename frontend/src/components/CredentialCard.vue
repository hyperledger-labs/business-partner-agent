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
      class="light-blue darken-3 card-title text-subtitle-1 white--text font-weight-medium"
      style="text-transform: capitalize"
      >{{ this.document.proofData.identifier.schemaLabel }}</v-card-title
    >
    <v-card-subtitle
      class="light-blue darken-3 card-title text-subtitle-2 white--text font-weight-thin"
      ><span v-if="unrevealedAttributes">
        {{ $t("component.credentialCard.notRevealed") }}</span
      ><span v-else-if="predicateProof">
        {{ $t("component.credentialCard.predicateProof") }}</span
      ><span v-else-if="selfAttestedAttributes">
        {{ $t("component.credentialCard.selfAttestedAttributes") }}</span
      ></v-card-subtitle
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
            <p class="font-italic">
              {{ unrevealedAttributes ? "***" : value }}
            </p>
          </v-col>
        </v-row>
      </v-container>
      <v-container>
        <v-row v-if="predicates">
          <v-col class="col-12">
            <p class="font-weight-medium">
              {{ predicates.name }}
              {{ translatePType }}
              {{ predicates.pvalue }}
            </p>
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
                  <v-list-item-subtitle>
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
import { RequestedProofType } from "@/constants";

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
  computed: {
    predicates() {
      return this.document.proofData.requestedPredicates;
    },
    unrevealedAttributes() {
      return (
        RequestedProofType.UNREVEALED_ATTRS ===
        this.document.proofData.proofType
      );
    },
    selfAttestedAttributes() {
      return (
        RequestedProofType.SELF_ATTESTED_ATTRS ===
        this.document.proofData.proofType
      );
    },
    predicateProof() {
      return (
        RequestedProofType.PREDICATES === this.document.proofData.proofType
      );
    },
    translatePType() {
      switch (this.predicates.ptype) {
        case "GREATER_THAN": {
          return ">";
        }
        case "GREATER_THAN_OR_EQUAL_TO": {
          return ">=";
        }
        case "LESS_THAN": {
          return "<";
        }
        case "LESS_THAN_OR_EQUAL_TO": {
          return "<=";
        }
        default: {
          return "";
        }
      }
    },
  },
};
</script>

<style scoped>
.v-application p {
  margin-bottom: 4px;
}
</style>
