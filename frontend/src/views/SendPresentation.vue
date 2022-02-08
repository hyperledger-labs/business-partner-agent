<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        {{ $t("view.sendPresentation.title") }}
      </v-card-title>

      <v-card-text>
        <h4 class="pt-4">{{ $t("view.sendPresentation.selectCredential") }}</h4>
        <MyCredentialList
          v-bind:headers="credHeaders"
          v-model="selectedCredentials"
          type="credential"
          selectable
        ></MyCredentialList>
      </v-card-text>

      <v-card-actions>
        <v-layout align-center align-end justify-end>
          <v-switch
            v-if="expertMode"
            v-model="useV2Exchange"
            :label="$t('button.useV2')"
          ></v-switch>
          <v-bpa-button color="secondary" @click="cancel()">{{
            $t("button.cancel")
          }}</v-bpa-button>
          <v-bpa-button
            :loading="this.isBusy"
            :disabled="this.selectedCredentials.length === 0"
            color="primary"
            @click="sendPresentation()"
            >{{ $t("button.submit") }}</v-bpa-button
          >
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import MyCredentialList from "@/components/MyCredentialList.vue";
import VBpaButton from "@/components/BpaButton";
import { ExchangeVersion } from "@/constants";

export default {
  name: "SendPresentation",
  components: {
    VBpaButton,
    MyCredentialList,
  },
  props: {
    id: String,
  },
  created() {
    EventBus.$emit("title", this.$t("view.sendPresentation.title"));
  },
  data: () => {
    return {
      isBusy: false,
      useV2Exchange: false,
      selectedCredentials: [],
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
    credHeaders() {
      return [
        {
          text: this.$t("view.sendPresentation.label"),
          value: "label",
        },
        {
          text: this.$t("view.sendPresentation.type"),
          value: "type",
        },
        {
          text: this.$t("view.sendPresentation.issuedBy"),
          value: "issuer",
        },
        {
          text: this.$t("view.sendPresentation.issuedAt"),
          value: "issuedAt",
        },
      ];
    },
  },
  methods: {
    sendPresentation() {
      this.isBusy = true;
      const selectedCredential = this.selectedCredentials[0].id;
      if (selectedCredential) {
        this.$axios
          .post(`${this.$apiBaseUrl}/partners/${this.id}/proof-send`, {
            myCredentialId: selectedCredential,
            exchangeVersion: this.useV2Exchange
              ? ExchangeVersion.V2
              : ExchangeVersion.V1,
          })
          .then((response) => {
            console.log(response);
            this.isBusy = false;
            EventBus.$emit(
              "success",
              this.$t("view.sendPresentation.eventSuccessSend")
            );
            this.$router.push({
              name: "Partner",
              params: { id: this.id },
            });
          })
          .catch((error) => {
            this.isBusy = false;
            EventBus.$emit("error", this.$axiosErrorMessage(error));
          });
      } else {
        this.isBusy = false;
      }
    },

    cancel() {
      this.$router.go(-1);
    },
  },
};
</script>

<style scoped>
.bg-light {
  background-color: #fafafa;
}

.bg-light-2 {
  background-color: #ececec;
}
</style>
