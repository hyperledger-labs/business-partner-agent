<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon> </v-btn
        >{{ $t("view.requestCredential.title") }}</v-card-title
      >
      <MyCredentialList
        v-bind:headers="docHeaders"
        v-model="selectedDocument"
        disable-verification-request
        selectable
        type="document"
      ></MyCredentialList>
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
          <v-bpa-button color="primary" @click="openCreateDocument()">
            {{ $t("view.requestCredential.createDocument") }}
          </v-bpa-button>
          <v-bpa-button
            :loading="this.isBusy"
            :disabled="selectedDocument.length === 0"
            color="primary"
            @click="submitCredentialRequest()"
          >
            {{ $t("view.requestCredential.sendRequest") }}
          </v-bpa-button>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import { ExchangeVersion } from "@/constants";
import MyCredentialList from "@/components/MyCredentialList";
import { docHeaders } from "@/components/tableHeaders/WalletHeaders";
import VBpaButton from "@/components/BpaButton";
import credentialService from "@/services/credentialService";

export default {
  name: "RequestPresentation",
  components: { MyCredentialList, VBpaButton },
  props: {
    id: String, // partner ID
  },
  mounted() {
    EventBus.$emit("title", this.$t("view.requestCredential.title"));
    this.$store.dispatch("loadDocuments");
    this.$store.dispatch("loadSchemas");
  },
  data: () => {
    return {
      isBusy: false,
      selectedDocument: [],
      selectedSchema: [],
      selectedIssuer: [],
      useV2Exchange: false,
      docHeaders: docHeaders,
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
  },
  methods: {
    openCreateDocument() {
      this.$router.push({
        name: "RequestCredentialCreateDocument",
        params: {
          id: this.id,
        },
      });
    },
    async submitCredentialRequest() {
      this.isBusy = true;
      let data = {
        documentId: this.selectedDocument[0].id,
        exchangeVersion: this.useV2Exchange
          ? ExchangeVersion.V2
          : ExchangeVersion.V1,
      };

      credentialService
        .sendCredentialRequest(this.id, data)
        .then(() => {
          EventBus.$emit("success", "Credential verification request sent");
          this.isBusy = false;
          this.$router.go(-1);
        })
        .catch((e) => {
          EventBus.$emit("error", this.$axiosErrorMessage(e));
          this.isBusy = false;
        });
    },
    cancel() {
      this.$router.go(-1);
    },
  },
};
</script>

<style>
.bg-light {
  background-color: #fafafa;
}
</style>
