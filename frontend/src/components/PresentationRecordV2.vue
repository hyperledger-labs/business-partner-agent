<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <!-- Valid/Invalid info for role verifier -->
    <v-container v-if="isStateVerified">
      <v-alert v-if="record.valid" dense border="left" type="success">
        {{ $t("view.presentationRecord.presentationValid") }}
      </v-alert>

      <v-alert v-else dense border="left" type="error">
        {{ $t("view.presentationRecord.presentationNotValid") }}
      </v-alert>
    </v-container>

    <!-- Request Content -->
    <h4 class="my-4">{{ $t("view.presentationRecord.requestContent") }}</h4>
    <v-container
      v-if="!isStateProposalSent"
      class="d-flex flex-wrap justify-space-between"
    >
      <!-- Requested Attributes -->
      <template
        v-for="([groupName, group], idx) in Object.entries(
          record.proofRequest['requestedAttributes']
        )"
      >
        <CredentialCard :key="groupName + idx" v-bind:document="group">
        </CredentialCard>
      </template>
    </v-container>

    <!-- About -->
    <v-card class="mb-3">
      <v-card-title height="40">{{
        $t("view.presentationRecord.about")
      }}</v-card-title>
      <v-divider></v-divider>

      <v-list dense>
        <v-list-item>
          <v-list-item-title
            class="grey--text text--darken-2 font-weight-medium"
          >
            {{ $t("view.presentationRecord.role") }}
          </v-list-item-title>
          <v-list-item-subtitle align="">
            {{ record.role | capitalize }}
          </v-list-item-subtitle>
        </v-list-item>
        <v-divider></v-divider>
        <v-list-item>
          <v-list-item-title
            class="grey--text text--darken-2 font-weight-medium"
          >
            {{ $t("view.presentationRecord.state") }}
          </v-list-item-title>
          <v-list-item-subtitle align="">
            {{
              (record.state ? record.state.replace("_", " ") : "") | capitalize
            }}
          </v-list-item-subtitle>
        </v-list-item>

        <v-divider></v-divider>

        <v-list-item>
          <v-list-item-title
            class="grey--text text--darken-2 font-weight-medium"
          >
            {{ $t("view.presentationRecord.requestName") }}
          </v-list-item-title>
          <v-list-item-subtitle>
            {{ record.proofRequest ? record.proofRequest.name : "" }}
          </v-list-item-subtitle>
        </v-list-item>
      </v-list>
    </v-card>

    <!-- Timeline  -->
    <Timeline v-bind:timeEntries="record.stateToTimestamp" />

    <!-- ExpertMode: Raw data -->
    <v-expansion-panels class="mt-4" v-if="expertMode" accordion flat>
      <v-expansion-panel>
        <v-expansion-panel-header
          class="grey--text text--darken-2 font-weight-medium bg-light"
          >{{ $t("showRawData") }}</v-expansion-panel-header
        >
        <v-expansion-panel-content class="bg-light">
          <vue-json-pretty :data="record"></vue-json-pretty>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-container>
</template>

<script lang="ts">
import {
  PresentationExchangeStates,
  Predicates,
  RequestTypes,
  Restrictions,
} from "@/constants";
import CredentialCard from "@/components/CredentialCard.vue";
import Timeline from "@/components/Timeline.vue";

export default {
  name: "PresentationRecord",
  props: {
    record: Object,
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
    isStateVerified() {
      return this.record.state === PresentationExchangeStates.VERIFIED;
    },
    isStateProposalSent() {
      return this.record.state === PresentationExchangeStates.PROPOSAL_SENT;
    },
    contentPanels: {
      get: function () {
        if (this.record.proofRequest) {
          const nPanels = RequestTypes.map((type) => {
            return Object.keys(this.record.proofRequest[type]).length;
          }).reduce((x, y) => x + y, 0);

          return this.record.state ===
            PresentationExchangeStates.REQUEST_RECEIVED
            ? [...Array.from({ length: nPanels }).keys()].map(
                (k, index) => index
              )
            : [];
        } else {
          return [];
        }
      },
    },
  },
  methods: {
    selectedCredential(group, credential) {
      group.cvalues = {};
      this.names(group).map((name) => {
        group.cvalues[name] = credential.credentialInfo.attrs[name];
      });
    },
    names(item) {
      return item.names ? item.names : [item.name];
    },
    toRestrictionLabel(restrType) {
      const index = Object.values(Restrictions).findIndex((restriction) => {
        return restriction.value === restrType;
      });
      return index !== -1
        ? Object.values(Restrictions)[index].label
        : restrType;
    },
    toCredentialLabel(matchedCred) {
      if (matchedCred.credentialInfo) {
        const credInfo = matchedCred.credentialInfo;

        if (credInfo.credentialLabel) {
          return `${credInfo.credentialLabel} (${credInfo.credentialId})`;
        } else if (credInfo.schemaLabel) {
          return credInfo.issuerLabel
            ? `${credInfo.schemaLabel} (${credInfo.credentialId}) - ${credInfo.issuerLabel}`
            : credInfo.schemaLabel;
        } else {
          return credInfo.credentialId;
        }
      } else {
        return this.$t(
          "view.presentationRecord.matchingCredentials.noInfoFound"
        );
      }
    },
    renderSchemaLabel(attributeGroupName) {
      // If groupName contains schema id, try to render label else show group name
      const end = attributeGroupName.lastIndexOf(".");

      if (end !== -1) {
        const schemaId = attributeGroupName.slice(0, Math.max(0, end + 2));
        const schema = this.$store.getters.getSchemas.find(
          (s) => s.schemaId === schemaId
        );

        return schema && schema.label
          ? `<strong>${schema.label}</strong><i>&nbsp;(${schema.schemaId})</i>`
          : attributeGroupName;
      }

      return attributeGroupName;
    },
  },
  data: () => {
    return {
      matchingCredentials: undefined,
      Predicates,
      Restrictions,
      RequestTypes,
    };
  },
  components: {
    CredentialCard,
    Timeline,
  },
};
</script>

<style scoped>
.v-btn {
  margin-left: 10px;
}
</style>
