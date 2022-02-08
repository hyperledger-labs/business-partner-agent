<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-expansion-panels accordion flat>
    <v-expansion-panel>
      <v-expansion-panel-header
        class="grey--text text--darken-2 font-weight-medium bg-light"
        >{{ $t("component.timeline.title") }}</v-expansion-panel-header
      >
      <v-expansion-panel-content class="bg-light">
        <v-timeline dense>
          <v-timeline-item
            fill-dot
            small
            v-for="item in timeEntries"
            :key="item.key"
          >
            <v-row class="pt-1">
              <v-col cols="3">
                {{ item[1] | formatDateLong }}
              </v-col>
              <v-col>
                <strong>
                  {{ item[0].replace("_", " ") | capitalize }}
                </strong>
                <div class="text-caption">
                  {{ item[2] }}
                </div>
              </v-col>
            </v-row>
          </v-timeline-item>
        </v-timeline>
      </v-expansion-panel-content>
    </v-expansion-panel>
  </v-expansion-panels>
</template>

<script lang="ts">
export default {
  name: "Timeline",
  props: {
    timeEntries: {
      type: [],
    },
  },
};
</script>
