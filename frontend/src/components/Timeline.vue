<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
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
            v-for="item in timeEntries"
            :key="item.key"
          >
            <v-row class="pt-1">
              <v-col cols="3">
                {{ item[1] | formatDateLong }}
              </v-col>
              <v-col>
                <div class="text-caption">
                  <strong>
                    {{ item[0].replace("_", " ") | capitalize }}
                  </strong>
                </div>
              </v-col>
            </v-row>
          </v-timeline-item>
        </v-timeline>
      </v-expansion-panel-content>
    </v-expansion-panel>
  </v-expansion-panels>
</template>

<script>
export default {
  name: "Timeline",
  props: {
    timeEntries: {
      type: Array[Object],
      default: () => "Title",
    },
  },
};
</script>
