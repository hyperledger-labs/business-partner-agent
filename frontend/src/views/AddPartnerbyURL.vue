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
          <v-col cols="4">
            <p class="grey--text text--darken-2 font-weight-medium">Set tags</p>
          </v-col>
          <v-col cols="6">
            <v-combobox
              multiple
              v-model="selectedTags"
              :items="tags"
              chips
              deletable-chips
            >
            </v-combobox>
          </v-col>

          <v-col cols="12">
            <v-bpa-button color="primary" @click="createInvitation()"
              >Generate Invitation (QR Code)</v-bpa-button
            >
          </v-col>
        </v-row>
        <v-row class="justify-center" v-else>
          <v-col>
            <div>
              <qrcode-vue
                class="d-flex justify-center"
                :value="invitationURL"
                :size="400"
                level="H"
              ></qrcode-vue>
              <template>
                <v-expansion-panels class="mt-4">
                  <v-expansion-panel>
                    <v-expansion-panel-header>
                      <span
                        class="grey--text text--darken-2 font-weight-medium"
                      >
                        Invitation URL</span
                      >
                    </v-expansion-panel-header>
                    <v-expansion-panel-content>
                      <span class="font-weight-light">{{ invitationURL }}</span>
                    </v-expansion-panel-content>
                  </v-expansion-panel>
                </v-expansion-panels>
              </template>
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
import store from "@/store";
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
      selectedTags: [],
    };
  },
  computed: {
    tags() {
      return this.$store.state.tags
        ? this.$store.state.tags.map((tag) => tag.name)
        : [];
    },
  },
  methods: {
    createInvitation() {
      let partnerToAdd = {
        alias: `${this.alias}`,
        tag: this.$store.state.tags.filter((tag) => {
          return this.selectedTags.includes(tag.name);
        }),
      };
      this.$axios
        .post(`${this.$apiBaseUrl}/partners/invitation`, partnerToAdd)
        .then((result) => {
          this.invitationURL = result.data.invitationUrl;

          if (result.status === 200 || result.status === 201) {
            store.dispatch("loadPartners");
            EventBus.$emit(
              "success",
              "Partner Invitation created successfully"
            );
          }
        })
        .catch((e) => {
          console.error(e);
          EventBus.$emit("error", e);
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
