<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-data-table
      :hide-default-footer="data.length < 10"
      v-model="selected"
      :loading="isBusy"
      :headers="headers"
      :items="filteredData"
      :show-select="selectable"
      :sort-by="['updatedAt']"
      :sort-desc="[true]"
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
import { CredentialTypes, PartnerStates } from "@/constants";
import { PartnerAPI, partnerService } from "@/services";

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
  created() {
    this.fetch();
  },
  data: () => {
    return {
      selected: new Array<any>(),
      data: new Array<PartnerAPI & { address: string }>(),
      isBusy: true,
      getPartnerState: getPartnerState,
    };
  },
  computed: {
    headers() {
      return [
        {
          text: this.$t("component.partnerList.headers.name"),
          value: "name",
        },
        this.showAllHeaders
          ? {
              text: this.$t("component.partnerList.headers.address"),
              value: "address",
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
    filteredData() {
      return !this.showInvitations
        ? this.data.filter((partner: PartnerAPI & { address: string }) => {
            return partner.state !== PartnerStates.INVITATION.value;
          })
        : this.data;
    },
    partnerNotifications() {
      return this.$store.getters.partnerNotifications;
    },
  },
  watch: {
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

    fetch() {
      this.$store.dispatch("loadPartnerSelectList");

      partnerService
        .getPartners(this.onlyIssuersForSchema)
        .then((result) => {
          console.log("Partner List", result);
          if (Object.prototype.hasOwnProperty.call(result, "data")) {
            this.isBusy = false;

            if (this.onlyAries) {
              result.data = result.data.filter((item) => {
                return item.ariesSupport === true;
              });
            }

            this.data = result.data.map((partner: PartnerAPI) => {
              const tempPartner: PartnerAPI & { address: string } = {
                address: this.getProfileAddress(partner),
                ...partner,
              };
              return tempPartner;
            });
          }
        })
        .catch((error) => {
          this.isBusy = false;
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
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
