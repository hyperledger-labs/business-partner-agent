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
          <v-col cols="12" v-if="useOutOfBand">
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title
                  class="grey--text text--darken-2 font-weight-medium"
                  >{{
                    $t("view.addPartnerbyURL.usePublicDid")
                  }}</v-list-item-title
                >
              </v-list-item-content>
              <v-list-item-action>
                <v-switch v-model="usePublicDid"></v-switch>
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
              <div>
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
                      <v-text-field
                        class="font-weight-light"
                        v-model="invitationURL"
                        readonly
                        outlined
                        dense
                        :label="$t('view.addPartnerbyURL.invitationURL')"
                        @blur="reset"
                      >
                        <template v-slot:append>
                          <v-tooltip top>
                            <template v-slot:activator="{ on, attrs }">
                              <v-btn
                                v-bind="attrs"
                                class="mr-0"
                                icon
                                v-on="on"
                                @click="copyInvitationURL"
                              >
                                <v-icon> $vuetify.icons.copy </v-icon>
                              </v-btn>
                            </template>
                            <span>{{ copyText }}</span>
                          </v-tooltip>
                        </template>
                      </v-text-field>
                    </v-expansion-panel-content>
                  </v-expansion-panel>
                </v-expansion-panels>
              </div>
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
import QrcodeVue from "qrcode.vue";
import VBpaButton from "@/components/BpaButton";
import store from "@/store";
import { invitationsService, TagAPI } from "@/services";
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
      copyText: "",
      msg: "",
      did: "",
      alias: "",
      partner: {},
      selectedTags: new Array<string>(),
      // Disable trust ping for invitation to
      // mobile wallets by default.
      trustPing: false,
      // Allows using Out of Band format for the invitation
      useOutOfBand: false,
      usePublicDid: true,
    };
  },
  created() {
    this.copyText = this.$t("button.clickToCopy");
  },
  computed: {
    tags() {
      return this.$store.getters.getTags
        ? this.$store.getters.getTags.map((tag: TagAPI) => tag.name)
        : [];
    },
  },
  methods: {
    createInvitation() {
      let partnerToAdd = {
        alias: `${this.alias}`,
        tag: this.$store.getters.getTags.filter((tag: TagAPI) => {
          return this.selectedTags.includes(tag.name);
        }),
        trustPing: this.trustPing,
        useOutOfBand: this.useOutOfBand,
        usePublicDid: this.useOutOfBand ? this.usePublicDid : undefined,
      };
      invitationsService
        .requestConnectionInvitation(partnerToAdd)
        .then((result) => {
          this.invitationURL = result.data.invitationUrl;

          if (result.status === 200 || result.status === 201) {
            store.dispatch("loadPartners");
            store.dispatch("loadPartnerSelectList");
            this.emitter.emit(
              "success",
              this.$t("view.addPartnerbyURL.eventSuccessCreatePartnerInvite")
            );
          }
        })
        .catch((error) => {
          this.emitter.emit("error", this.$axiosErrorMessage(error));
        });
    },
    async copyInvitationURL() {
      await navigator.clipboard.writeText(this.invitationURL);
      this.copyText = this.$t("button.copied");
    },
    reset() {
      this.copyText = this.$t("button.clickToCopy");
    },
  },
};
</script>
<style scoped>
span {
  width: 100%;
}
</style>
