<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="my-4">
      <v-card-title class="bg-light"
        >Schemas for Issuing Credentials</v-card-title
      >
      <v-card-text>
        <SchemaList
          :manage-credential-definitions="true"
          :manage-trusted-issuers="false"
          @changed="onSchemasChanged"
        />
      </v-card-text>
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-dialog v-model="createSchemaDialog" persistent max-width="600px">
            <template v-slot:activator="{ on, attrs }">
              <v-bpa-button v-bind="attrs" v-on="on" color="secondary"
                >Create Schema</v-bpa-button
              >
            </template>
            <CreateSchema
              @success="onSchemaCreated"
              @cancelled="createSchemaDialog = false"
            />
          </v-dialog>
          <v-dialog v-model="addSchemaDialog" persistent max-width="600px">
            <template v-slot:activator="{ on, attrs }">
              <v-bpa-button v-bind="attrs" v-on="on" color="primary"
                >Import Schema</v-bpa-button
              >
            </template>
            <AddSchema
              @success="onSchemaAdded"
              @cancelled="addSchemaDialog = false"
            />
          </v-dialog>
        </v-layout>
      </v-card-actions>
    </v-card>
    <v-card class="my-4">
      <v-card-title class="bg-light">Issued Credentials</v-card-title>
      <v-card-text>
        <CredExList
          :items="issuedCredentials"
          :is-loading="isLoadingCredentials"
        ></CredExList>
      </v-card-text>
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-autocomplete
            label="Select partner"
            v-model="partner"
            :items="partners"
            return-object
            class="mx-4"
            flat
            single-line
            hide-no-data
            hide-details
            dense
            outlined
            clearable
            clear-icon="$vuetify.icons.delete"
          >
            <template v-slot:item="data">
              <v-icon
                x-small
                class="ml-2 mr-2"
                :color="partnerStateColor(data.item)"
                >$vuetify.icons.partnerState</v-icon
              >
              {{ data.item.text }}
            </template>
            <template v-slot:selection="data">
              <v-icon
                x-small
                class="ml-2 mr-2"
                :color="partnerStateColor(data.item)"
                >$vuetify.icons.partnerState</v-icon
              >
              {{ data.item.text }}
            </template>
          </v-autocomplete>
          <v-autocomplete
            label="Select credential"
            v-model="credDef"
            :items="credDefs"
            return-object
            class="mx-4"
            flat
            single-line
            hide-no-data
            hide-details
            dense
            outlined
            clearable
            clear-icon="$vuetify.icons.delete"
          ></v-autocomplete>
          <v-dialog
            v-model="issueCredentialDialog"
            persistent
            max-width="600px"
          >
            <template v-slot:activator="{ on, attrs }">
              <v-bpa-button
                v-bind="attrs"
                v-on="on"
                color="primary"
                :disabled="issueCredentialDisabled"
                >Issue Credential</v-bpa-button
              >
            </template>
            <IssueCredential
              :credDefId="credDefId"
              :partnerId="partnerId"
              :credDefList="credDefs"
              :partnerList="partners"
              @success="credentialIssued"
              @cancelled="issueCredentialDialog = false"
            >
            </IssueCredential>
          </v-dialog>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import { issuerService, partnerService } from "@/services";
import AddSchema from "@/components/AddSchema";
import CreateSchema from "@/components/CreateSchema";
import CredExList from "@/components/CredExList";
import IssueCredential from "@/components/IssueCredential";
import SchemaList from "@/components/SchemaList";
import * as textUtils from "@/utils/textUtils";
import * as partnerUtils from "@/utils/partnerUtils";
import store from "@/store";
import VBpaButton from "@/components/BpaButton";

export default {
  name: "CredentialManagement",
  components: {
    VBpaButton,
    SchemaList,
    AddSchema,
    CreateSchema,
    IssueCredential,
    CredExList,
  },
  created() {
    EventBus.$emit("title", "Credential Management");
    this.loadCredentials();
  },
  data: () => {
    return {
      isLoadingCredentials: false,
      issuedCredentials: [],
      partners: [],
      partner: {},
      partnerId: "",
      credDefs: [],
      credDef: {},
      credDefId: "",
      issueCredentialDisabled: true,
      issueCredentialDialog: false,
      addSchemaDialog: false,
      createSchemaDialog: false,
    };
  },
  computed: {},
  watch: {
    partner(val) {
      this.issueCredentialDisabled =
        !val || !val.id || !this.credDef || !this.credDef.id;
      this.partnerId = val ? val.id : "";
    },
    credDef(val) {
      this.issueCredentialDisabled =
        !val || !val.id || !this.partner || !this.partner.id;
      this.credDefId = val ? val.id : "";
    },
  },
  methods: {
    partnerStateColor(p) {
      return partnerUtils.getPartnerStateColor(p.state);
    },
    async loadCredDefs() {
      const cresp = await issuerService.listCredDefs();
      if (cresp.status === 200) {
        this.credDefs = cresp.data.map((c) => {
          return {
            value: c.id,
            text: c.displayText,
            fields: c.schema.schemaAttributeNames.map((key) => {
              return {
                type: key,
                label: textUtils.schemaAttributeLabel(key),
              };
            }),
            ...c,
          };
        });
      }
    },
    async loadCredentials() {
      this.isLoadingCredentials = true;
      this.issuedCredentials = [];
      this.partners = [];
      this.partner = {};
      this.credDefs = [];
      this.credDef = {};

      // get partner list
      const presp = await partnerService.listPartners();
      if (presp.status === 200) {
        this.partners = presp.data.map((p) => {
          return { value: p.id, text: p.name, ...p };
        });
      }

      // get list of schema/creddefs
      await this.loadCredDefs();

      const iresp = await issuerService.listCredentialExchangesAsIssuer();
      if (iresp.status === 200) {
        this.issuedCredentials = iresp.data;
      }
      this.isLoadingCredentials = false;
    },
    credentialIssued() {
      this.issueCredentialDialog = false;
      this.loadCredentials();
    },
    onSchemaAdded() {
      store.dispatch("loadSchemas");
      this.addSchemaDialog = false;
    },
    onSchemaCreated() {
      store.dispatch("loadSchemas");
      this.createSchemaDialog = false;
    },
    onSchemasChanged() {
      // rebuild the cred defs
      this.loadCredDefs();
    },
  },
};
</script>
