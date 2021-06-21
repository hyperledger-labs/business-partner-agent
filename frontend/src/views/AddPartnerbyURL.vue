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
        <v-row v-if="!invitationURL">
          <v-col cols="12">
            <v-bpa-button color="secondary" @click="createInvitation()"
              >Generate QR Code</v-bpa-button
            >
          </v-col>
        </v-row>
        <v-row v-else>
          <v-col>
            <div>
              <qrcode-vue
                :value="invitationURL"
                :size="400"
                level="H"
              ></qrcode-vue>
              <br />
              <br />
              <span class="font-weight-light">{{ invitationURL }}</span>
            </div>
          </v-col>
        </v-row>
      </v-container>
      <v-card-actions>
        <v-layout justify-space-between>
          <v-bpa-button color="secondary" to="/app/partners"
            >Return</v-bpa-button
          >
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
import QrcodeVue from "qrcode.vue";
import VBpaButton from "@/components/BpaButton";
export default {
  name: "AddPartnerbyURL",
  created: () => {},
  components: {
    VBpaButton,
    QrcodeVue,
  },
  data: () => {
    return {
      partnerLoading: false,
      partnerLoaded: false,
      invitationURL: "",
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

          if (result.status === 201) {
            //   this.$axios.get(`${this.$apiBaseUrl}/partners/${result.data.id}`).then( res => {
            //       this.partnerLoaded = true
            //       this.partnerLoading = false

            //   });
            // } else {
            //   this.partnerLoading = false;
            EventBus.$emit(
              "success",
              "Partner Invitation created successfully"
            );
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
<style scoped>
span {
  width: 100%;
}
</style>
