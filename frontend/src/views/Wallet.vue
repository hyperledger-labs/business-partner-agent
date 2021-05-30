<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="my-4">
      <v-card-title class="bg-light"> Documents </v-card-title>
      <MyCredentialList
        v-bind:headers="docHeaders"
        type="document"
      ></MyCredentialList>
      <v-card-actions>
        <v-menu>
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
        </v-menu>
      </v-card-actions>
    </v-card>
    <v-card class="my-10">
      <v-card-title class="bg-light">Verified Credentials</v-card-title>
      <MyCredentialList
        v-bind:headers="credHeaders"
        type="credential"
        :indicateNew="true"
      ></MyCredentialList>
    </v-card>
  </v-container>
</template>

<script>
import { CredentialTypes } from "../constants";
import MyCredentialList from "@/components/MyCredentialList";
import { EventBus } from "../main";
import {
  credHeaders,
  docHeaders,
} from "@/components/tableHeaders/WalletHeaders";

export default {
  name: "Wallet",
  components: {
    MyCredentialList,
  },
  created() {
    EventBus.$emit("title", "Wallet");
    this.$store.dispatch("loadDocuments");
    this.$store.dispatch("loadSchemas");
  },
  data: () => {
    return {
      search: "",
      scheams: [],
      credHeaders: credHeaders,
      docHeaders: docHeaders,
    };
  },
  methods: {
    createDocument: function (documentType) {
      documentType =
        documentType && documentType.type
          ? documentType
          : CredentialTypes.UNKNOWN;
      this.$router.push({
        name: "DocumentAdd",
        params: {
          type: documentType.type,
          schemaId: documentType.schemaId,
        },
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
};
</script>

<style scoped>
.truncate {
  max-width: 1px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
