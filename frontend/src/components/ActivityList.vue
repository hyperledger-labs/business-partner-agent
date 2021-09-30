<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-layout align-end justify-end>
      <v-combobox
        label="Filter by"
        v-model="filter"
        :items="filterList"
        class="mx-4"
        single-line
        hide-no-data
        hide-details
        return-object
        flat
        dense
        outlined
        clearable
        clear-icon="$vuetify.icons.delete"
      ></v-combobox>
      <v-combobox
        v-model="filterValue"
        :items="filterValueList"
        class="mx-4"
        single-line
        hide-no-data
        hide-details
        return-object
        flat
        dense
        outlined
        clearable
        clear-icon="$vuetify.icons.delete"
      ></v-combobox>
      <v-bpa-button color="primary" @click="fetchItems()">Refresh</v-bpa-button>
    </v-layout>
    <v-data-table
      :hide-default-footer="items.length < 10"
      :loading="isBusy"
      :headers="headers"
      :items="items"
      single-select
      :sort-by="['updatedAt']"
      :sort-desc="[true]"
      @click:row="openItem"
    >
      <template v-slot:[`item.indicator`]="{ item }">
        <new-message-icon :type="'task'" :id="item.id"></new-message-icon>
      </template>
      <template v-slot:[`item.partner`]="{ item }">
        {{ partnerLabel(item.partner) }}
      </template>

      <template v-slot:[`item.type`]="{ item }">
        {{ activityTypeLabel(item.type) }}
      </template>

      <template v-slot:[`item.state`]="{ item }">
        {{ activityStateLabel(item.state) }}
      </template>

      <template v-slot:[`item.role`]="{ item }">
        {{ activityRoleLabel(item.role) }}
      </template>

      <template v-slot:[`item.updatedAt`]="{ item }">
        {{ item.updatedAt | moment("YYYY-MM-DD HH:mm") }}
      </template>
    </v-data-table>
  </v-container>
</template>
<script>
import { EventBus } from "@/main";
import { ActivityRoles, ActivityStates, ActivityTypes } from "@/constants";
import VBpaButton from "@/components/BpaButton";
import activitiesService from "@/services/activitiesService";
import NewMessageIcon from "@/components/NewMessageIcon";

export default {
  name: "ActivityList",
  components: { VBpaButton, NewMessageIcon },
  props: {
    activities: {
      type: Boolean,
      default: () => false,
    },
    tasks: {
      type: Boolean,
      default: () => true,
    },
    headers: {
      type: Array,
      default: () => [
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
          text: "State",
          value: "state",
        },
      ],
    },
  },
  mounted() {
    this.filter = null;
    this.filterValue = null;
    this.filterValueList = [];
    this.fetchItems();
  },
  data: () => {
    return {
      isBusy: true,
      items: [],
      filter: null,
      filterList: [{ text: "Type", value: "type" }],
      filterValue: null,
      filterValueList: [],
    };
  },
  watch: {
    filter(val) {
      this.filterValue = null;
      this.filterValueList = [];
      if (val && val.value === "type") {
        this.filterValueList = [];
        for (let k in ActivityTypes) {
          this.filterValueList.push({
            text: ActivityTypes[k].label,
            value: ActivityTypes[k].value,
          });
        }
      }
    },
  },
  methods: {
    fetchItems() {
      let filter = undefined;
      if (this.filter && this.filterValue) {
        filter = { name: this.filter.value, value: this.filterValue.value };
      }
      activitiesService
        .listActivities(this.tasks, this.activities, filter)
        .then((result) => {
          if ({}.hasOwnProperty.call(result, "data")) {
            this.isBusy = false;
            this.items = result.data;
          }
        })
        .catch((e) => {
          this.isBusy = false;
          if (e.response.status === 404) {
            this.items = [];
          } else {
            EventBus.$emit("error", this.$axiosErrorMessage(e));
          }
        });
    },
    openItem(item) {
      // if we click on it, mark it seen...
      this.$store.commit("taskNotificationSeen", { id: item.id });

      console.log(item);

      if (item.type === ActivityTypes.CONNECTION_REQUEST.value) {
        this.$router.push({
          name: "Partner",
          params: {
            id: item.linkId,
          },
        });
      } else if (item.type === ActivityTypes.PRESENTATION_EXCHANGE.value) {
        let route = {
          name: "Partner",
          params: {
            id: item.partner.id,
            presExId: item.linkId,
          },
        };

        this.$router.push(route);
      }
    },
    activityTypeLabel(type) {
      const o = ActivityTypes[type.toUpperCase()];
      return o ? o.label : type;
    },
    activityStateLabel(state) {
      const o = ActivityStates[state.toUpperCase()];
      return o ? o.label : state;
    },
    activityRoleLabel(role) {
      const o = ActivityRoles[role.toUpperCase()];
      return o ? o.label : role;
    },
    partnerLabel(partner) {
      return partner ? partner.name : "Unknown";
    },
  },
};
</script>
