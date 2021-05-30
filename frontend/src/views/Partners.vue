<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto px-8">
      <v-row class="d-flex align-end flex-column">
        <v-switch
          v-model="showInvitations"
          inset
          label="Show Invitations"
        ></v-switch>
      </v-row>

      <PartnerList
        :headers="headers"
        :indicateNew="true"
        :showInvitations="showInvitations"
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
import { EventBus } from "../main";
import PartnerList from "@/components/PartnerList";
export default {
  name: "Partners",
  components: {
    PartnerList,
  },
  created() {
    EventBus.$emit("title", "Business Partners");
  },
  data: () => {
    return {
      isBusy: true,
      search: "",
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
          text: "Last Updated",
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
