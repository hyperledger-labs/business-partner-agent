<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container justify-center>
    <v-card class="my-4 mx-auto">
      <v-card-title class="bg-light">
        {{ $t("view.partners.title") }}
        <v-layout justify-end>
          <v-switch
            class="mr-4"
            v-model="showInvitations"
            inset
            :label="$t('view.partners.showInvitations')"
          ></v-switch>
          <v-bpa-button color="primary" icon @click="refresh = true">
            <v-icon dark>$vuetify.icons.refresh</v-icon>
          </v-bpa-button>
        </v-layout>
      </v-card-title>
      <PartnerList
        show-all-headers
        :indicateNew="true"
        :showInvitations="showInvitations"
        :refresh="refresh"
        @refreshed="refresh = false"
      />
      <v-card-actions>
        <v-btn
          color="primary"
          small
          dark
          absolute
          bottom
          left
          fab
          :to="{ name: 'AddPartner' }"
        >
          <v-icon>$vuetify.icons.add</v-icon>
        </v-btn>
        <v-btn
          color="primary"
          small
          dark
          absolute
          bottom
          left
          fab
          style="margin-left: 50px"
          :to="{ name: 'AddPartnerbyURL' }"
        >
          <v-icon>$vuetify.icons.qrCode</v-icon>
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import VBpaButton from "@/components/BpaButton";
import PartnerList from "@/components/PartnerList.vue";
export default {
  name: "Partners",
  components: {
    PartnerList,
    VBpaButton,
  },
  created() {
    EventBus.$emit("title", this.$t("view.partners.title"));
  },
  data: () => {
    return {
      search: "",
      refresh: false,
      showInvitations: false,
      partners: [],
    };
  },
};
</script>
