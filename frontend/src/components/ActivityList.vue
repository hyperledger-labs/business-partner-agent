<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-layout align-end justify-end>
      <v-combobox
        :label="$t('component.activityList.labelFilterCombobox')"
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
      <v-bpa-button color="primary" @click="fetchItems()">{{
        $t("button.refresh")
      }}</v-bpa-button>
    </v-layout>
    <v-data-table
      :hide-default-footer="hideFooter"
      :loading="isBusy"
      :headers="headers"
      :items="items"
      :options.sync="options"
      :server-items-length="totalNumberOfElements"
      sort-by="updatedAt"
      sort-desc
      single-select
      @click:row="openItem"
    >
      <template v-slot:[`item.indicator`]="{ item }">
        <new-message-icon
          :type="newMessageIconType"
          :id="item.id"
        ></new-message-icon>
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
        {{ item.updatedAt | formatDateLong }}
      </template>
    </v-data-table>
  </v-container>
</template>
<script lang="ts">
import { EventBus } from "@/main";
import { ActivityRoles, ActivityStates, ActivityTypes } from "@/constants";
import VBpaButton from "@/components/BpaButton";
import NewMessageIcon from "@/components/NewMessageIcon.vue";
import {
  ActivityItem,
  ActivityType,
  PartnerAPI,
  activitiesService,
  PageOptions,
} from "@/services";

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
    showRole: {
      type: Boolean,
      default: () => false,
    },
  },
  mounted() {
    this.filter = undefined;
    this.filterValue = undefined;
    this.filterValueList = [];
  },
  data: () => {
    return {
      isBusy: true,
      items: new Array<ActivityItem>(),
      filter: undefined as { text: string; value: string },
      filterValue: undefined as { text: string; value: string },
      filterValueList: new Array<{ text: string; value: string }>(),
      hideFooter: true,
      options: {},
      totalNumberOfElements: 0,
    };
  },
  watch: {
    filter(value: { text: string; value: string }) {
      this.filterValue = undefined;
      this.filterValueList = [];
      if (value && value.value === "type") {
        this.filterValueList = [];
        for (let k in ActivityTypes) {
          this.filterValueList.push({
            text: ActivityTypes[k as ActivityType].label,
            value: ActivityTypes[k as ActivityType].value,
          });
        }
      }
    },
    options: {
      handler() {
        this.fetchItems();
      },
    },
  },
  computed: {
    filterList() {
      return [
        {
          text: this.headers.find(
            (x: { text: string; value: string }) => x.value === "type"
          ).text,
          value: "type",
        },
      ];
    },
    headers() {
      return [
        {
          text: "",
          value: "indicator",
          sortable: false,
          filterable: false,
        },
        {
          text: this.$t("component.activityList.tableHeaders.type"),
          value: "type",
        },
        {
          text: this.$t("component.activityList.tableHeaders.partner"),
          value: "partner",
        },
        {
          text: this.$t("component.activityList.tableHeaders.updatedAt"),
          value: "updatedAt",
        },
        this.showRole
          ? {
              text: this.$t("component.activityList.tableHeaders.role"),
              value: "role",
            }
          : {},
        {
          text: this.$t("component.activityList.tableHeaders.state"),
          value: "state",
        },
      ];
    },
    newMessageIconType() {
      return this.tasks ? "task" : "activity";
    },
  },
  methods: {
    async fetchItems() {
      this.isBusy = true;
      this.items = [];
      let filter;
      if (this.filter && this.filterValue) {
        filter = { name: this.filter.value, value: this.filterValue.value };
      }
      const params = PageOptions.toUrlSearchParams(this.options);
      try {
        const response = await activitiesService.listActivities(
          this.tasks,
          this.activities,
          filter,
          params
        );
        if (response.status === 200) {
          const { itemsPerPage } = this.options;
          this.items = response.data.content;
          this.totalNumberOfElements = response.data.totalSize;
          this.hideFooter = this.totalNumberOfElements <= itemsPerPage;
        }
      } catch (error) {
        EventBus.$emit("error", this.$axiosErrorMessage(error));
      }
      this.isBusy = false;
    },
    openItem(item: ActivityItem) {
      // if we click on it, mark it seen...
      this.$store.commit("activityNotificationSeen", { id: item.id });
      this.$store.commit("taskNotificationSeen", { id: item.id });

      console.log(item);

      switch (item.type.toUpperCase()) {
        case ActivityTypes.CONNECTION_REQUEST.value: {
          this.$router.push({
            name: "Partner",
            params: {
              id: item.linkId,
            },
          });

          break;
        }
        case ActivityTypes.CREDENTIAL_EXCHANGE.value: {
          if (
            item.role === ActivityRoles.CREDENTIAL_EXCHANGE_ISSUER.value ||
            item.state === ActivityStates.CREDENTIAL_EXCHANGE_RECEIVED.value
          ) {
            // this isn't a credential... either we issued it, or it is just at an offer state
            let route = {
              name: "Partner",
              params: {
                id: item.partner.id,
                credExId: item.linkId,
              },
            };

            this.$router.push(route);
          } else {
            let route = {
              name: "Credential",
              params: {
                id: item.linkId,
                type: "credential",
              },
            };

            this.$router.push(route);
          }

          break;
        }
        case ActivityTypes.PRESENTATION_EXCHANGE.value: {
          let route = {
            name: "Partner",
            params: {
              id: item.partner.id,
              presExId: item.linkId,
            },
          };

          this.$router.push(route);

          break;
        }
        // No default
      }
    },
    activityTypeLabel(type: string) {
      const o: { value: string; label: string } =
        ActivityTypes[type.toUpperCase() as keyof unknown];
      return o ? o.label : type;
    },
    activityStateLabel(state: string) {
      const o: { value: string; label: string } =
        ActivityStates[state.toUpperCase() as keyof unknown];
      return o ? o.label : state;
    },
    activityRoleLabel(role: string) {
      const o: { value: string; label: string } =
        ActivityRoles[role.toUpperCase() as keyof unknown];
      return o ? o.label : role;
    },
    partnerLabel(partner: PartnerAPI) {
      return partner
        ? partner.name
        : this.$t("component.activityList.labelPartnerUnknown");
    },
  },
};
</script>
