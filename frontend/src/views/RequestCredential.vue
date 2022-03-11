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
          <v-icon dark>$vuetify.icons.prev</v-icon> </v-btn
        >{{ $t("view.requestCredential.title") }}</v-card-title
      >
      <MyCredentialList
        v-bind:headers="docHeaders"
        v-model="selectedDocument"
        disable-verification-request
        use-indy
        use-json-ld
        selectable
        type="document"
      ></MyCredentialList>
      <v-card-actions>
        <v-layout align-center align-end justify-end>
          <v-switch
            v-if="showV2Slider"
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

<script lang="ts">
import { EventBus } from "@/main";
import { ExchangeVersion, CredentialTypes } from "@/constants";
import MyCredentialList from "@/components/MyCredentialList.vue";
import VBpaButton from "@/components/BpaButton";
import credentialService from "@/services/credential-service";

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
    };
  },
  computed: {
    showV2Slider() {
      return (
        this.expertMode &&
        this.selectedDocument[0] &&
        this.selectedDocument[0].type !== CredentialTypes.JSON_LD.type
      );
    },
    expertMode() {
      return this.$store.state.expertMode;
    },
    docHeaders() {
      return [
        {
          text: this.$t("view.requestCredential.headers.label"),
          value: "label",
        },
        {
          text: this.$t("view.requestCredential.headers.type"),
          value: "type",
        },
        {
          text: this.$t("view.requestCredential.headers.createdDate"),
          value: "createdDate",
        },
        {
          text: this.$t("view.requestCredential.headers.updatedDate"),
          value: "updatedDate",
        },
        {
          text: this.$t("view.requestCredential.headers.isPublic"),
          value: "isPublic",
        },
      ];
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
          EventBus.$emit(
            "success",
            this.$t("view.requestCredential.eventSuccessSend")
          );
          this.isBusy = false;
          this.$router.go(-1);
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
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
