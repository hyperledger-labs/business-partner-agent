<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-data-table
      :hide-default-footer="hideFooter"
      v-model="selected"
      :loading="isBusy"
      :headers="headers"
      :items="items"
      :show-select="selectable"
      :options.sync="options"
      :server-items-length="totalNumberOfElements"
      sort-by="updatedAt"
      sort-desc
      single-select
      @click:row="open"
    >
      <template v-slot:[`item.name`]="{ item }">
        <new-message-icon :type="'partner'" :id="item.id"></new-message-icon>
        <PartnerStateIndicator
          v-if="item.state"
          v-bind:state="item.state"
        ></PartnerStateIndicator>
        <span v-bind:class="{ 'font-weight-medium': item.new }">
          {{ item.name }}
        </span>
        <v-chip class="ml-2" v-for="tag in item.tag" :key="tag.id">{{
          tag.name
        }}</v-chip>
      </template>

      <template v-slot:[`item.address`]="{ item }">
        <span text-truncate>
          {{ item.address }}
        </span>
      </template>

      <template v-slot:[`item.createdAt`]="{ item }">
        {{ item.createdAt | formatDateLong }}
      </template>

      <template v-slot:[`item.updatedAt`]="{ item }">
        {{ item.updatedAt | formatDateLong }}
      </template>

      <template v-slot:[`item.state`]="{ item }">
        {{ getPartnerState(item).label }}
      </template>
    </v-data-table>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import { getPartnerState } from "@/utils/partnerUtils";
import PartnerStateIndicator from "@/components/PartnerStateIndicator.vue";
import NewMessageIcon from "@/components/NewMessageIcon.vue";
import { CredentialTypes } from "@/constants";
import { PartnerAPI, partnerService, PageOptions } from "@/services";

export default {
  name: "PartnerList",
  components: {
    PartnerStateIndicator,
    NewMessageIcon,
  },
  props: {
    showAllHeaders: {
      type: Boolean,
      default: false,
    },
    selectable: {
      type: Boolean,
      default: false,
    },
    onlyAries: {
      type: Boolean,
      default: false,
    },
    showInvitations: {
      type: Boolean,
      default: false,
    },
    indicateNew: {
      type: Boolean,
      default: false,
    },
    onlyIssuersForSchema: {
      type: String,
      default: "",
    },
    refresh: {
      type: Boolean,
      default: false,
    },
  },
  data: () => {
    return {
      selected: new Array<any>(),
      items: new Array<PartnerAPI & { address: string }>(),
      isBusy: true,
      getPartnerState: getPartnerState,
      hideFooter: true,
      options: {},
      totalNumberOfElements: 0,
    };
  },
  computed: {
    headers() {
      return [
        {
          text: this.$t("component.partnerList.headers.name"),
          value: "name",
          sortable: false,
        },
        this.showAllHeaders
          ? {
              text: this.$t("component.partnerList.headers.address"),
              value: "address",
              sortable: false,
            }
          : {},
        this.showAllHeaders
          ? {
              text: this.$t("component.partnerList.headers.updatedAt"),
              value: "updatedAt",
            }
          : {},
        this.showAllHeaders
          ? {
              text: this.$t("component.partnerList.headers.state"),
              value: "state",
            }
          : {},
      ];
    },
    expertMode() {
      return this.$store.getters.getExpertMode;
    },
    partnerNotifications() {
      return this.$store.getters.partnerNotifications;
    },
  },
  watch: {
    options: {
      handler() {
        this.fetch();
      },
    },
    showInvitations: {
      handler() {
        this.fetch();
        this.$set(this.options, "page", 1);
      },
    },
    refresh: function (newValue: boolean) {
      if (newValue) {
        this.fetch();
        this.$emit("refreshed");
      }
    },
    partnerNotifications: function (newValue: any) {
      if (newValue) {
        // TODO: Don't fetch all partners but only add new partner
        this.fetch();
      }
    },
  },
  methods: {
    open(partner: PartnerAPI & { address: string }) {
      this.$router.push({
        name: "Partner",
        params: {
          id: partner.id,
        },
      });
    },
    async fetch() {
      this.isBusy = true;
      this.items = [];
      const params = PageOptions.toUrlSearchParams(this.options);
      try {
        const response = await partnerService.getAllWithoutInvites(
          this.showInvitations,
          params
        );
        if (response.status === 200) {
          const { itemsPerPage } = this.options;
          this.items = response.data.content.map((partner: PartnerAPI) => {
            const tempPartner: PartnerAPI & { address: string } = {
              address: this.getProfileAddress(partner),
              ...partner,
            };
            return tempPartner;
          });
          this.totalNumberOfElements = response.data.totalSize;
          this.hideFooter = this.totalNumberOfElements <= itemsPerPage;
        }
      } catch (error) {
        EventBus.$emit("error", this.$axiosErrorMessage(error));
      }
      this.isBusy = false;
    },
    getProfileAddress(partner: PartnerAPI) {
      if (partner.credential && partner.credential.length > 0) {
        const profile: any = partner.credential.find((item) => {
          return item.type === CredentialTypes.PROFILE.type;
        });
        let address = "";
        if (profile) {
          const registeredSiteAddress =
            profile.credentialData.registeredSite.address;
          if (registeredSiteAddress.city !== "") {
            address = registeredSiteAddress.city;
          }
          if (registeredSiteAddress.zipCode !== "") {
            address = registeredSiteAddress.zipCode + " " + address;
          }
          if (registeredSiteAddress.streetAddress !== "") {
            address = registeredSiteAddress.streetAddress + ", " + address;
          }
        }
        return address;
      }
      return "";
    },
  },
};
</script>
