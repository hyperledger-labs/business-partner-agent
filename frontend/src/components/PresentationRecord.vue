<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-list-item>
      <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
        {{ $t("view.presentationRecord.role") }}
      </v-list-item-title>
      <v-list-item-subtitle align="">
        {{ record.role | capitalize }}
      </v-list-item-subtitle>
    </v-list-item>

    <v-list-item>
      <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
        {{ $t("view.presentationRecord.state") }}
      </v-list-item-title>
      <v-list-item-subtitle align="">
        {{ (record.state ? record.state.replace("_", " ") : "") | capitalize }}
      </v-list-item-subtitle>
    </v-list-item>

    <v-list-item>
      <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
        {{ $t("view.presentationRecord.requestName") }}
      </v-list-item-title>
      <v-list-item-subtitle align="">
        {{ record.proofRequest ? record.proofRequest.name : "" }}
      </v-list-item-subtitle>
    </v-list-item>
    <!-- Timeline  -->
    <Timeline :time-entries="record.stateToTimestamp"></Timeline>

    <!-- Request Content -->
    <template v-if="!isStateProposalSent">
      <h4 class="my-4">{{ $t("view.presentationRecord.requestContent") }}:</h4>

      <!-- Requested Attributes -->

      <v-expansion-panels v-model="contentPanels" multiple accordion flat>
        <template v-for="type in RequestTypes">
          <v-expansion-panel
            v-for="([groupName, group], idx) in Object.entries(
              record.proofRequest[type]
            )"
            :key="groupName + idx"
          >
            <v-expansion-panel-header
              class="grey--text text--darken-2 font-weight-medium bg-light"
            >
              <span v-html="renderSchemaLabel(groupName)"></span
            ></v-expansion-panel-header>
            <v-expansion-panel-content class="bg-light">
              <v-list-item
                v-if="group.proofData && group.proofData.identifier"
                class="ml-n4 mb-4"
              >
                <v-list-item-title>
                  <strong>{{ $t("view.presentationRecord.issuer") }}</strong>
                </v-list-item-title>
                <v-list-item-subtitle>
                  {{ group.proofData.identifier.issuerLabel }}
                </v-list-item-subtitle>
              </v-list-item>

              <h4 class="mb-4">
                {{ $t("view.presentationRecord.dataFields") }}
              </h4>

              <template v-if="type === 'requestedAttributes'">
                <v-list-item v-for="name in names(group)" :key="name">
                  <v-list-item-title>
                    {{ name }}
                  </v-list-item-title>
                  <v-list-item-subtitle v-if="group.cvalues">
                    {{ group.cvalues[name] }}
                  </v-list-item-subtitle>
                  <v-list-item-subtitle v-else-if="group.proofData">
                    {{ group.proofData.revealedAttributes[name] }}
                  </v-list-item-subtitle>
                </v-list-item>
              </template>

              <template v-if="type === 'requestedPredicates'">
                <v-list-item>
                  <v-list-item-title>
                    {{ group.name }} {{ Predicates[group.ptype] }}
                    {{ group.pvalue }}
                  </v-list-item-title>
                  <v-list-item-subtitle v-if="group.cvalues">
                    {{ group.cvalues[group.name] }}
                  </v-list-item-subtitle>
                  <v-list-item-subtitle v-else-if="group.proofData">
                  </v-list-item-subtitle>
                </v-list-item>
              </template>

              <!-- Restrictions -->

              <v-expansion-panels accordion flat class="ml-n6">
                <v-expansion-panel>
                  <v-expansion-panel-header class="bg-light"
                    ><strong>{{
                      $t("view.presentationRecord.restrictions")
                    }}</strong></v-expansion-panel-header
                  >
                  <v-expansion-panel-content class="bg-light">
                    <v-list-item-group
                      v-for="(restr, idy) in group.restrictions"
                      :key="100 + idy"
                    >
                      <v-list-item
                        v-for="([restrType, restrValue], idz) in Object.entries(
                          restr
                        )"
                        :key="restrType + idz"
                      >
                        <v-list-item-title>
                          {{ toRestrictionLabel(restrType) }}
                        </v-list-item-title>
                        <v-list-item-subtitle>{{
                          restrValue
                        }}</v-list-item-subtitle>
                      </v-list-item>
                    </v-list-item-group>
                  </v-expansion-panel-content>
                </v-expansion-panel>
              </v-expansion-panels>

              <!-- Select matching credential -->

              <div v-if="group.matchingCredentials">
                <h4 class="mb-4">
                  {{
                    $t(
                      "view.presentationRecord.matchingCredentials.selectHeader"
                    )
                  }}
                </h4>
                <v-select
                  :label="
                    $t('view.presentationRecord.matchingCredentials.label')
                  "
                  return-object
                  :items="group.matchingCredentials"
                  :item-text="toCredentialLabel"
                  v-model="group.selectedCredential"
                  outlined
                  @change="selectCredential(group, $event)"
                  dense
                ></v-select>
              </div>
            </v-expansion-panel-content>
          </v-expansion-panel>
        </template>
      </v-expansion-panels>
    </template>

    <!-- Valid/Invalid info for role verifier -->

    <v-container v-if="isStateVerified">
      <v-alert v-if="record.valid" dense border="left" type="success">
        {{ $t("view.presentationRecord.presentationValid") }}
      </v-alert>

      <v-alert v-else dense border="left" type="error">
        {{ $t("view.presentationRecord.presentationNotValid") }}
      </v-alert>
    </v-container>

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
    selectCredential(group, credential) {
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
        let revokedLabel = "";
        if (credInfo.revoked) {
          revokedLabel = this.$t(
            "view.presentationRecord.matchingCredentials.credentialRevoked"
          );
        }

        if (credInfo.credentialLabel) {
          return `${credInfo.credentialLabel} (${credInfo.credentialId}) ${revokedLabel}`;
        } else if (credInfo.schemaLabel) {
          return credInfo.issuerLabel
            ? `${credInfo.schemaLabel} (${credInfo.credentialId}) - ${credInfo.issuerLabel} ${revokedLabel}`
            : `${credInfo.schemaLabel} ${revokedLabel}`;
        } else {
          return `${credInfo.credentialId} ${revokedLabel}`;
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
  components: { Timeline },
};
</script>

<style scoped>
.v-btn {
  margin-left: 10px;
}
</style>
