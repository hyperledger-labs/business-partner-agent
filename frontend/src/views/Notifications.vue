<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto px-8">
      <v-data-table
          :hide-default-footer="tasks.length < 10"
          :loading="isBusy"
          :headers="headers"
          :items="tasks"
          single-select
          :sort-by="['updatedAt']"
          :sort-desc="[true]"
          @click:row="open"
      >
        <template v-slot:[`item.updatedAt`]="{ item }">
          {{ item.updatedAt | moment("YYYY-MM-DD HH:mm") }}
        </template>

      </v-data-table>
      <v-card-actions>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";

export default {
  name: "Notifications",
  components: { },
  created() {
    EventBus.$emit("title", this.$t("view.notifications.title"));
  },
  mounted() {
    this.fetchTasks();
  },
  data: () => {
    return {
      isBusy: true,
      headers: [
        {
          text: "Type",
          value: "type",
        },
        {
          text: "Connection",
          value: "connectionAlias",
        },
        {
          text: "Last Updated",
          value: "updatedAt",
        },
        {
          text: "State",
          value: "state",
        },
      ],
      tasks: [],
    };
  },
  methods: {
    fetchTasks() {
      this.$axios
        .get(`${this.$apiBaseUrl}/activities?task=true&activity=false`)
        .then((result) => {
          if ({}.hasOwnProperty.call(result, "data")) {
            this.isBusy = false;
            this.tasks = result.data;
          }
        })
        .catch((e) => {
          this.isBusy = false;
          if (e.response.status === 404) {
            this.data = [];
          } else {
            console.error(e);
            EventBus.$emit("error", e);
          }
        });
    },
    open(item) {
      if (item.type === "connection_invitation") {
        this.$router.push({
          name: "Partner",
          params: {
            id: item.linkId,
          },
        });
      }
    },

  },
};
</script>
