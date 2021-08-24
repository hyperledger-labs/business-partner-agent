<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
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

    <!-- Content based on template data -->

    <h4 class="mt-4">Requested Information: {{ record.proofTemplate.name }}</h4>

    <attribute-group
      v-if="record.proofTemplate"
      v-bind:requestData="record.proofTemplate.attributeGroups"
    >
    </attribute-group>

    <!-- Raw content if no template -->
    <v-container v-else>
      <v-alert type="warning" border="left">
        Presentation Request could not be parsed into template
      </v-alert>
      <v-expansion-panels accordion flat>
        <v-expansion-panel>
          <v-expansion-panel-header
            class="grey--text text--darken-2 font-weight-medium bg-light"
            >Show raw request</v-expansion-panel-header
          >
          <v-expansion-panel-content class="bg-light">
            <vue-json-pretty :data="record.proofRequest"></vue-json-pretty>
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
import AttributeGroup from "@/components/proof-templates/AttributeGroup";

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
      return this.record.state === "verified";
    },
  },
  methods: {},
  data: () => {
    return {
      matchingCredentials: null,
    };
  },
  components: {
    AttributeGroup,
  },
};
</script>

<style scoped>
.v-btn {
  margin-left: 10px;
}
</style>
