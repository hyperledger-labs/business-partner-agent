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
        selectable
        type="document"
      ></MyCredentialList>
      <v-card-actions>
        <v-layout align-center align-end justify-end>
          <v-bpa-button color="secondary" @click="cancel()">{{
            $t("button.cancel")
          }}</v-bpa-button>
          <v-bpa-button color="primary" @click="openCreateProofTemplate()">
            {{ $t("view.requestCredential.createDocument") }}
          </v-bpa-button>
          <v-bpa-button
            :loading="this.isBusy"
            :disabled="selectedProofTemplate.length === 0"
            color="primary"
            @click="submitRequest()"
          >
            {{ $t("view.requestCredential.sendRequest") }}
          </v-bpa-button>
        </v-layout>

        <!-- <v-menu>
          <template v-slot:activator="{ on, attrs }">
            <v-btn
                color="primary"
                dark
                small
                absolute
                bottom
                left
                fab
                v-bind="attrs"
                v-on="on"
            >
              <v-icon>$vuetify.icons.add</v-icon>
            </v-btn>
          </template>
          <v-list>
            <v-list-item
                v-for="(type, i) in newDocumentTypes"
                :key="i"
                @click="createDocument(type)"
            >
              <v-list-item-title>{{ type.label }}</v-list-item-title>
            </v-list-item>
          </v-list>
        </v-menu> -->
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import proofTemplateService from "@/services/proofTemplateService";
import { ExchangeVersion } from "@/constants";
import MyCredentialList from "@/components/MyCredentialList";
import { docHeaders } from "@/components/tableHeaders/WalletHeaders";
import VBpaButton from "@/components/BpaButton";

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
      selectedProofTemplate: [],
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
    openCreateProofTemplate() {
      this.$router.push({
        name: "RequestPresentationCreateProofTemplate",
        params: {
          id: this.id,
        },
      });
    },
    async submitRequest() {
      this.isBusy = true;
      let data = {
        exchangeVersion: this.useV2Exchange
          ? ExchangeVersion.V2
          : ExchangeVersion.V1,
      };

      proofTemplateService
        .sendProofTemplate(this.selectedProofTemplate[0].id, this.id, data)
        .then(() => {
          EventBus.$emit("success", "Presentation request sent");
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
