<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-select
      :label="$t('view.requestCredential.createDocumentView.selectSchemaLabel')"
      return-object
      v-model="selectedSchema"
      :items="newDocumentTypes"
      outlined
    >
      <template v-slot:item="{ item }">
        <strong>{{ item.label }}</strong
        >&nbsp;<em>({{ item.schemaId }})</em>
      </template>
      <template v-slot:selection="{ item }">
        <strong>{{ item.label }}</strong
        >&nbsp;<em>({{ item.schemaId }})</em>
      </template>
    </v-select>
    <Document
      v-if="selectedSchema"
      :key="selectedSchema.schemaId"
      :schema-id="selectedSchema.schemaId"
      :type="selectedSchema.type"
      disable-verification-request
      enable-v2-switch
      :create-button-label="$t('button.saveAndSend')"
      v-on:received-document-id="submitRequest($event)"
    ></Document>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import Document from "@/views/Document.vue";
import { CredentialTypes, ExchangeVersion } from "@/constants";
import credentialService from "@/services/credentialService";

export default {
  name: "RequestCredentialCreateDocument",
  components: { Document },
  props: {
    id: String, // partner ID
  },
  data: () => {
    return {
      selectedSchema: undefined,
    };
  },
  methods: {
    async submitRequest(documentIdAndExchangeVersion) {
      this.isBusy = true;
      let data = {
        documentId: documentIdAndExchangeVersion.documentId,
        exchangeVersion: documentIdAndExchangeVersion.useV2Exchange
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
        .catch((e) => {
          EventBus.$emit("error", this.$axiosErrorMessage(e));
          this.isBusy = false;
        });
    },
  },
  computed: {
    newDocumentTypes() {
      let docTypes = this.$store.getters.getSchemas;
      if (this.$store.getters.getOrganizationalProfile) {
        docTypes = docTypes.filter(
          (schema) => schema.type !== CredentialTypes.PROFILE.type
        );
      }
      return docTypes;
    },
  },
  mounted() {
    EventBus.$emit("title", this.$t("view.requestPresentation.title"));
  },
};
</script>
