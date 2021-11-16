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
    ></Document>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import Document from "@/views/Document.vue";
import { CredentialTypes } from "@/constants";

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
