<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-data-table
      :hide-default-footer="data.length < 10"
      v-model="selected"
      :loading="isBusy"
      :headers="headers"
      :items="data"
      :show-select="selectable"
      :sort-by="['updatedAt']"
      :sort-desc="[true]"
      single-select
      @click:row="open"
    >
      <template v-slot:[`item.name`]="{ item }">
        <new-message-icon
          v-show="item.new"
          isPartner
          :text="item.name"
        ></new-message-icon>
        <PartnerStateIndicator
          v-if="item.state"
          v-bind:state="item.state"
        ></PartnerStateIndicator>
        <span v-bind:class="{ 'font-weight-medium': item.new }">
          {{ item.name }}
        </span>
      </template>

      <template v-slot:[`item.createdAt`]="{ item }">
        {{ item.createdAt | moment("YYYY-MM-DD HH:mm") }}
      </template>

      <template v-slot:[`item.updatedAt`]="{ item }">
        {{ item.updatedAt | moment("YYYY-MM-DD HH:mm") }}
      </template>
    </v-data-table>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
import { getPartnerName } from "../utils/partnerUtils";
import PartnerStateIndicator from "@/components/PartnerStateIndicator";
import NewMessageIcon from "@/components/NewMessageIcon";

export default {
  name: "PartnerList",
  components: {
    PartnerStateIndicator,
    NewMessageIcon,
  },
  props: {
    selectable: {
      type: Boolean,
      default: false,
    },
    headers: {
      type: Array,
      default: () => [
        {
          text: "Name",
          value: "name",
        },
      ],
    },
    onlyAries: {
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
  },
  created() {
    this.fetch();
  },
  data: () => {
    return {
      selected: [],
      data: [],
      isBusy: true,
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
  },
  methods: {
    open(partner) {
      this.$router.push({
        name: "Partner",
        params: {
          id: partner.id,
        },
      });
    },
    fetch() {
      // Query only for partners that can issue credentials of specified schema
      let queryParam = "";
      if (this.onlyIssuersForSchema.length > 0) {
        queryParam = `?schemaId=${this.onlyIssuersForSchema}`;
      }
      this.$axios
        .get(`${this.$apiBaseUrl}/partners${queryParam}`)
        .then((result) => {
          console.log("Partner List", result);
          if ({}.hasOwnProperty.call(result, "data")) {
            this.isBusy = false;

            result.data = this.markNew(result.data);

            if (this.onlyAries) {
              result.data = result.data.filter((item) => {
                return item.ariesSupport === true;
              });
            }

            this.data = result.data.map((partner) => {
              partner.name = getPartnerName(partner);
              return partner;
            });

            console.log(this.data);
          }
        })
        .catch((e) => {
          this.isBusy = false;

          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    markNew(data) {
      if (this.indicateNew) {
        let newPartners = this.$store.getters.newPartners;
        if (Object.keys(newPartners).length > 0) {
          data = data.map((partner) => {
            if ({}.hasOwnProperty.call(newPartners, partner.id)) {
              partner.new = true;
            }
            return partner;
          });
        }
      }
      return data;
    },
  },
};
</script>
