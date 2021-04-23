<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card max-width="600" class="mx-auto" flat>
      <v-card-title class="grey--text text--darken-2">
        Add new Business Partner
      </v-card-title>
      <v-container>
        <v-row class="mx-2">
          <v-col cols="4">
            <p class="grey--text text--darken-2 font-weight-medium">
              Set a name
            </p>
          </v-col>
          <v-col cols="8">
            <v-text-field
              label="Name"
              placeholder=""
              v-model="alias"
              outlined
              dense
            >
            </v-text-field>
          </v-col>
        </v-row>
        <v-row v-if="!invitationURL">
          <v-col cols="12">
              <v-btn color="Primary" text @click="createInvitation()">GenerateURL</v-btn>
          </v-col>
        </v-row>
        <v-row v-else>
          <v-layout justify-center>
            <span class="font-weight-medium">{{ invitationURL }}</span>
          </v-layout>
        </v-row>      
      </v-container>
      <v-card-actions>
        <v-layout justify-space-between>
          <v-btn color="secondary" text to="/app/partners">Return</v-btn>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
export default {
  name: "AddPartnerbyURL",
  created: () => {},
  data: () => {
    return {
      partnerLoading: false,
      partnerLoaded: false,
      invitationURL:"",
      msg: "",
      did: "",
      alias: "",
      partner: {},
    };
  },
  methods: {
    createInvitation() {
      let partnerToAdd = {
        alias: `${this.alias}`,
      };
      this.$axios
        .post(`${this.$apiBaseUrl}/partners/invitation`, partnerToAdd)
        .then((result) => {
          console.log(result);
          this.invitationURL = result.data.invitationUrl;
          console.log(this.invitationURL);

          if (result.status === 201) {
            //   this.$axios.get(`${this.$apiBaseUrl}/partners/${result.data.id}`).then( res => {
            //       console.log(res);
            //       this.partnerLoaded = true
            //       this.partnerLoading = false

            //   });
            // } else {
            //   this.partnerLoading = false;
            EventBus.$emit("success", "Partner Invitation created successfully");
          }
        })
        .catch((e) => {
          if (e.response.status === 412) {
            EventBus.$emit("error", "Partner already exists");
          } else {
            console.error(e);
            EventBus.$emit("error", e);
          }
        });
    },
  },
};
</script>
