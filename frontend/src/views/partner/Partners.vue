<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

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
            label="Show Invitations"
          ></v-switch>
          <v-bpa-button color="primary" icon @click="refresh = true">
            <v-icon dark>$vuetify.icons.refresh</v-icon>
          </v-bpa-button>
        </v-layout>
      </v-card-title>

      <PartnerList
        :headers="headers"
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

<script>
import { EventBus } from "@/main";
import VBpaButton from "@/components/BpaButton";
import PartnerList from "@/components/PartnerList";
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
      headers: [
        {
          text: "Name",
          value: "name",
        },
        {
          text: "Address",
          value: "address",
        },
        {
          text: "Updated at",
          value: "updatedAt",
        },
        {
          text: "State",
          value: "state",
        },
      ],
      partners: [],
    };
  },
  methods: {},
};
</script>
