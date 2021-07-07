<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="my-4">
      <v-card-title class="bg-light">{{ $t("view.notifications.tasks.title") }}</v-card-title>
      <v-card-text>
        <activity-list :activities="false" :tasks="true" />
      </v-card-text>
      <v-card-actions>
      </v-card-actions>
    </v-card>
    <v-card class="my-4">
      <v-card-title class="bg-light">{{ $t("view.notifications.activities.title") }}</v-card-title>
      <v-card-text>
        <activity-list :activities="true" :tasks="false" />
      </v-card-text>
      <v-card-actions>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import ActivityList from "@/components/ActivityList";

export default {
  name: "Notifications",
  components: {ActivityList},
  created() {
    EventBus.$emit("title", this.$t("view.notifications.title"));
  },
};
</script>
