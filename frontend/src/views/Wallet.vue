<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="my-10">
      <v-card-title class="bg-light"
        >{{ $t("view.wallet.credentials.title")
        }}<v-layout justify-end>
          <v-bpa-button
            color="primary"
            icon
            @click="$refs.myCredentialList.fetch()"
          >
            <v-icon dark>$vuetify.icons.refresh</v-icon>
          </v-bpa-button>
        </v-layout></v-card-title
      >
      <MyCredentialList
        v-bind:headers="credHeaders"
        type="credential"
        :indicateNew="true"
        ref="myCredentialList"
      ></MyCredentialList>
    </v-card>
    <v-card class="my-4">
      <v-card-title class="bg-light"
        >{{ $t("view.wallet.documents.title") }}
        <v-layout justify-end>
          <v-bpa-button
            color="primary"
            icon
            @click="$refs.myDocumentList.fetch()"
          >
            <v-icon dark>$vuetify.icons.refresh</v-icon>
          </v-bpa-button>
        </v-layout></v-card-title
      >
      <MyCredentialList
        v-bind:headers="docHeaders"
        type="document"
        ref="myDocumentList"
      ></MyCredentialList>
      <v-card-actions>
        <v-menu>
          <template v-slot:activator="{ on, attrs }">
            <v-btn
              v-bind="attrs"
              color="primary"
              dark
              small
              absolute
              bottom
              left
              fab
              v-on="on"
            >
              <v-icon>$vuetify.icons.add</v-icon>
            </v-btn>
          </template>
          <v-list max-height="50vh" class="overflow-y-auto">
            <v-list-item
              v-for="(schema, idx) in newDocumentTypes"
              :key="idx"
              @click="createDocument(schema)"
            >
              <v-list-item-content>
                <v-list-item-title>
                  {{ schema.label }}
                </v-list-item-title>
                <v-list-item-subtitle>
                  {{ schema.schemaId }}
                </v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
          </v-list>
        </v-menu>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { CredentialTypes } from "@/constants";
import MyCredentialList from "@/components/MyCredentialList.vue";
import { EventBus } from "@/main";
import VBpaButton from "@/components/BpaButton";
import { SchemaAPI } from "@/services";

export default {
  name: "Wallet",
  components: {
    MyCredentialList,
    VBpaButton,
  },
  created() {
    EventBus.$emit("title", this.$t("view.wallet.title"));
    this.$store.dispatch("loadDocuments");
    this.$store.dispatch("loadSchemas");
  },
  data: () => {
    return {
      search: "",
    };
  },
  methods: {
    createDocument: function (document: SchemaAPI) {
      this.$router.push({
        name: "DocumentAdd",
        params: {
          type: document.type,
          schemaId: document.schemaId,
        },
      });
    },
  },
  computed: {
    credHeaders() {
      return [
        {
          text: this.$t("view.wallet.credentials.headers.label"),
          value: "label",
        },
        {
          text: this.$t("view.wallet.credentials.headers.type"),
          value: "type",
        },
        {
          text: this.$t("view.wallet.credentials.headers.issuer"),
          value: "issuer",
        },
        {
          text: this.$t("view.wallet.credentials.headers.issuedAt"),
          value: "issuedAt",
          sortable: false,
        },
        {
          text: this.$t("view.wallet.credentials.headers.revoked"),
          value: "revoked",
        },
        {
          text: this.$t("view.wallet.credentials.headers.isPublic"),
          value: "isPublic",
        },
      ];
    },
    docHeaders() {
      return [
        {
          text: this.$t("view.wallet.documents.headers.label"),
          value: "label",
        },
        {
          text: this.$t("view.wallet.documents.headers.type"),
          value: "type",
        },
        {
          text: this.$t("view.wallet.documents.headers.createdDate"),
          value: "createdAt",
        },
        {
          text: this.$t("view.wallet.documents.headers.updatedDate"),
          value: "updatedAt",
        },
        {
          text: this.$t("view.wallet.documents.headers.isPublic"),
          value: "isPublic",
        },
      ];
    },
    newDocumentTypes() {
      let documentTypes = this.$store.getters.getSchemas;
      if (this.$store.getters.getOrganizationalProfile) {
        documentTypes = documentTypes.filter(
          (schema: SchemaAPI) => schema.type !== CredentialTypes.PROFILE.type
        );
      }
      return documentTypes;
    },
  },
};
</script>
