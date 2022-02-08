<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card max-width="600" class="mx-auto" flat>
      <v-card-title class="grey--text text--darken-2">
        {{ $t("view.addPartnerbyURL.title") }}
      </v-card-title>
      <v-container>
        <v-row v-if="!invitationURL">
          <v-col cols="4">
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title
                  class="grey--text text--darken-2 font-weight-medium"
                >
                  {{ $t("view.addPartner.setName") }}
                </v-list-item-title>
              </v-list-item-content>
            </v-list-item>
          </v-col>
          <v-col cols="8">
            <v-text-field
              :label="$t('view.addPartnerbyURL.labelName')"
              v-model="alias"
              outlined
              dense
            >
            </v-text-field>
          </v-col>
          <v-col cols="4">
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title
                  class="grey--text text--darken-2 font-weight-medium"
                >
                  {{ $t("view.addPartner.setTags") }}
                </v-list-item-title>
              </v-list-item-content>
            </v-list-item>
          </v-col>
          <v-col cols="8">
            <v-autocomplete
              multiple
              v-model="selectedTags"
              :items="tags"
              chips
              deletable-chips
            >
            </v-autocomplete>
          </v-col>
          <v-col cols="12">
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title
                  class="grey--text text--darken-2 font-weight-medium"
                  >{{ $t("view.addPartner.trustPing") }}</v-list-item-title
                >
              </v-list-item-content>
              <v-list-item-action>
                <v-switch v-model="trustPing"></v-switch>
              </v-list-item-action>
            </v-list-item>
          </v-col>
          <v-col cols="12">
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title
                  class="grey--text text--darken-2 font-weight-medium"
                  >{{ $t("view.addPartnerbyURL.useOOB") }}</v-list-item-title
                >
              </v-list-item-content>
              <v-list-item-action>
                <v-switch v-model="useOutOfBand"></v-switch>
              </v-list-item-action>
            </v-list-item>
          </v-col>
          <v-col cols="12">
            <v-bpa-button color="primary" @click="createInvitation()">{{
              $t("view.addPartnerbyURL.createInvitation")
            }}</v-bpa-button>
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
                        {{ $t("view.addPartnerbyURL.invitationURL") }}</span
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
          <v-bpa-button color="secondary" to="/app/partners">{{
            $t("button.return")
          }}</v-bpa-button>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import QrcodeVue from "qrcode.vue";
import VBpaButton from "@/components/BpaButton";
import store from "@/store";
export default {
  name: "AddPartnerbyURL",
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
      // Disable trust ping for invitation to
      // mobile wallets by default.
      trustPing: false,
      // Allow to use Out of Band format for invitation
      useOutOfBand: false,
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
        trustPing: this.trustPing,
        useOutOfBand: this.useOutOfBand,
      };
      this.$axios
        .post(`${this.$apiBaseUrl}/invitations`, partnerToAdd)
        .then((result) => {
          this.invitationURL = result.data.invitationUrl;

          if (result.status === 200 || result.status === 201) {
            store.dispatch("loadPartners");
            store.dispatch("loadPartnerSelectList");
            EventBus.$emit(
              "success",
              this.$t("view.addPartnerbyURL.eventSuccessCreatePartnerInvite")
            );
          }
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
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
