<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="my-4">
      <v-card-title class="bg-light"
        >{{ $t("view.notifications.tasks.title") }}
        <v-btn icon @click="showTasks = !showTasks" style="margin-left: auto">
          <v-icon v-if="showTasks">$vuetify.icons.up</v-icon>
          <v-icon v-else>$vuetify.icons.down</v-icon>
        </v-btn>
      </v-card-title>
      <v-expand-transition>
        <div v-show="showTasks">
          <v-card-text>
            <activity-list :activities="false" :tasks="true" />
          </v-card-text>
        </div>
      </v-expand-transition>
    </v-card>
    <v-card class="my-4">
      <v-card-title class="bg-light"
        >{{ $t("view.notifications.activities.title") }}
        <v-btn
          icon
          @click="showActivities = !showActivities"
          style="margin-left: auto"
        >
          <v-icon v-if="showActivities">$vuetify.icons.up</v-icon>
          <v-icon v-else>$vuetify.icons.down</v-icon>
        </v-btn>
      </v-card-title>
      <v-expand-transition>
        <div v-show="showActivities">
          <v-card-text>
            <activity-list
              :activities="true"
              :tasks="false"
              :headers="activityHeaders"
            />
          </v-card-text>
        </div>
      </v-expand-transition>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import ActivityList from "@/components/ActivityList";

export default {
  name: "Notifications",
  components: { ActivityList },
  created() {
    EventBus.$emit("title", this.$t("view.notifications.title"));
    // do we want to clear all the notifications?
    //this.$store.commit("taskNotificationsClear");
  },
  data: () => {
    return {
      activityHeaders: [
        {
          text: "",
          value: "indicator",
          sortable: false,
          filterable: false,
        },
        {
          text: "Type",
          value: "type",
        },
        {
          text: "Connection",
          value: "partner",
        },
        {
          text: "Update at",
          value: "updatedAt",
        },
        {
          text: "Role",
          value: "role",
        },
        {
          text: "State",
          value: "state",
        },
      ],
      showTasks: true,
      showActivities: false,
    };
  },
};
</script>
