<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-list-item>
      <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
        Role
      </v-list-item-title>
      <v-list-item-subtitle align="">
        {{ record.role }}
      </v-list-item-subtitle>
    </v-list-item>

    <v-list-item>
      <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
        State
      </v-list-item-title>
      <v-list-item-subtitle align="">
        {{ record.state ? record.state.replace("_", " ") : "" }}
      </v-list-item-subtitle>
    </v-list-item>

    <v-list-item>
      <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
        Request Name
      </v-list-item-title>
      <v-list-item-subtitle align="">
        {{ record.proofTemplate ? record.proofTemplate.name : "" }}
      </v-list-item-subtitle>
    </v-list-item>
    <!-- Timeline  -->
    <v-expansion-panels accordion flat>
      <v-expansion-panel>
        <v-expansion-panel-header
          class="grey--text text--darken-2 font-weight-medium bg-light"
          >Timeline</v-expansion-panel-header
        >
        <v-expansion-panel-content class="bg-light">
          <v-timeline dense>
            <v-timeline-item
              fill-dot
              small
              v-for="item in Object.entries(record.stateToTimestamp)"
              :key="item.key"
            >
              <v-row class="pt-1">
                <v-col cols="3">
                  {{ item[1] | formatDateLong }}
                </v-col>
                <v-col>
                  <div class="text-caption">
                    <strong> {{ item[0].replace("_", " ") }} </strong>
                  </div>
                </v-col>
              </v-row>
            </v-timeline-item>
          </v-timeline>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>

    <!-- Request Content -->
    <v-container class="mb-4">
      <h4 class="my-4">Request Content:</h4>

      <!-- Requested Attributes -->

      <v-expansion-panels v-model="contentPanels" accordion flat>
        <v-expansion-panel
          v-for="([groupKey, group], idx) in Object.entries(
            record.proofRequest.requestedAttributes
          )"
          :key="idx"
        >
          <v-expansion-panel-header
            class="grey--text text--darken-2 font-weight-medium bg-light"
            >{{ groupKey }}</v-expansion-panel-header
          >
          <v-expansion-panel-content class="bg-light">
            <v-list-item
              v-if="group.proofData && group.proofData.identifier"
              class="ml-n4 mb-4"
            >
              <v-list-item-title>
                <strong>Issuer</strong>
              </v-list-item-title>
              <v-list-item-subtitle>
                {{ group.proofData.identifier.issuerLabel }}
              </v-list-item-subtitle>
            </v-list-item>

            <h4 class="mb-4">Data fields</h4>

            <v-list-item v-for="name in names(group)" :key="name">
              <v-list-item-title>
                {{ toName(name) }}
              </v-list-item-title>
              <v-list-item-subtitle v-if="group.cvalues">
                {{ toName(group.cvalues[name]) }}
              </v-list-item-subtitle>
              <v-list-item-subtitle v-else-if="group.proofData">
                {{ group.proofData.revealedAttributes[name] }}
              </v-list-item-subtitle>
            </v-list-item>

            <!-- Restrictions -->

            <v-expansion-panels accordion flat class="ml-n6">
              <v-expansion-panel>
                <v-expansion-panel-header class="bg-light"
                  ><strong>Restrictions</strong></v-expansion-panel-header
                >

                <v-expansion-panel-content class="bg-light">
                  <v-list-item-group
                    v-for="(restr, idy) in group.restrictions"
                    :key="idy"
                  >
                    <v-list-item
                      v-for="[restrType, restrValue] in Object.entries(restr)"
                      :key="restrType"
                    >
                      <v-list-item-title>
                        {{
                          Object.values(Restrictions)[
                            Object.values(Restrictions).findIndex(
                              (restriction) => {
                                return restriction.value === restrType;
                              }
                            )
                          ].label
                        }}
                      </v-list-item-title>
                      <v-list-item-subtitle>{{
                        restrValue
                      }}</v-list-item-subtitle>
                    </v-list-item>
                  </v-list-item-group>
                </v-expansion-panel-content>
              </v-expansion-panel>
            </v-expansion-panels>

            <div v-if="group.matchingCredentials">
              <h4 class="mb-4">Select data for presentation</h4>
              <v-select
                label="Matching Credentials"
                return-object
                :items="group.matchingCredentials"
                item-text="credentialInfo.credentialLabel"
                v-model="group.selectedCredential"
                outlined
                @change="selectedCredential(group, $event)"
                dense
              ></v-select>
            </div>
          </v-expansion-panel-content>
        </v-expansion-panel>

        <!-- Requested Predicates -->

        <v-expansion-panel
          v-for="([groupName, group], idx) in Object.entries(
            record.proofRequest.requestedPredicates
          )"
          :key="idx"
        >
          <v-expansion-panel-header
            class="grey--text text--darken-2 font-weight-medium bg-light"
            >{{ groupName }}</v-expansion-panel-header
          >
          <v-expansion-panel-content class="bg-light">
            <v-list-item
              v-if="group.proofData && group.proofData.identifier"
              class="ml-n4 mb-4"
            >
              <v-list-item-title>
                <strong>Issuer</strong>
              </v-list-item-title>
              <v-list-item-subtitle>
                {{ group.proofData.identifier.issuerLabel }}
              </v-list-item-subtitle>
            </v-list-item>

            <h4 class="mb-4">Data fields</h4>

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

            <v-expansion-panels accordion flat class="ml-n6">
              <v-expansion-panel>
                <v-expansion-panel-header class="bg-light"
                  ><strong>Restrictions</strong></v-expansion-panel-header
                >

                <v-expansion-panel-content class="bg-light">
                  <v-list-item-group
                    v-for="(restr, idy) in group.restrictions"
                    :key="idy"
                  >
                    <v-list-item
                      v-for="[restrType, restrValue] in Object.entries(restr)"
                      :key="restrType"
                    >
                      <v-list-item-title>
                        {{
                          Object.values(Restrictions)[
                            Object.values(Restrictions).findIndex(
                              (restriction) => {
                                return restriction.value === restrType;
                              }
                            )
                          ].label
                        }}
                      </v-list-item-title>
                      <v-list-item-subtitle>{{
                        restrValue
                      }}</v-list-item-subtitle>
                    </v-list-item>
                  </v-list-item-group>
                </v-expansion-panel-content>
              </v-expansion-panel>
            </v-expansion-panels>

            <div v-if="group.matchingCredentials">
              <h4 class="mb-4">Select data for presentation</h4>
              <v-select
                label="Matching Credentials"
                return-object
                :items="group.matchingCredentials"
                item-text="credentialInfo.credentialLabel"
                v-model="group.selectedCredential"
                outlined
                @change="selectedCredential(group, $event)"
                dense
              ></v-select>
            </div>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
    </v-container>

    <!-- Valid/Invalid info for role verifier -->

    <v-container v-if="isStateVerified">
      <v-alert v-if="record.valid" dense border="left" type="success">
        Presentation is valid
      </v-alert>

      <v-alert v-else dense border="left" type="error">
        Presentation is not valid
      </v-alert>
    </v-container>

    <!-- ExpertMode: Raw data -->

    <v-expansion-panels v-if="expertMode" accordion flat>
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

<script>
import {
  PresentationExchangeStates,
  Predicates,
  RequestTypes,
  Restrictions,
} from "@/constants";
// import AttributeGroup from "@/components/proof-templates/AttributeGroup";
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

          if (
            this.record.state === PresentationExchangeStates.REQUEST_RECEIVED
          ) {
            return nPanels === 1
              ? 0
              : [...Array(nPanels).keys()].map((k, i) => i);
          } else {
            return [];
          }
        } else {
          return [];
        }
      },
      set: function () {},
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
    toName(name) {
      if (name.startsWith("attr::")) {
        const end = name.lastIndexOf("::");
        return name.substring("attr::".length, end);
      } else {
        return name;
      }
    },
  },
  data: () => {
    return {
      matchingCredentials: null,
      Predicates,
      Restrictions,
    };
  },
  components: {
    // AttributeGroup,
  },
};
</script>

<style scoped>
.v-btn {
  margin-left: 10px;
}
</style>
