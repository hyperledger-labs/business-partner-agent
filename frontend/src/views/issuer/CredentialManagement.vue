<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="my-4" style="display: none">
      <v-card-title class="bg-light">Schemas for Issuing Credentials </v-card-title>
      <v-card-text>
        <v-data-table></v-data-table>
      </v-card-text>
      <v-card-actions>
      </v-card-actions>
    </v-card>
    <v-card class="my-4">
      <v-card-title class="bg-light">Issued Credentials</v-card-title>
      <v-card-text>
        <CredExList
            v-bind:items="issuedCredentials"
        ></CredExList>
      </v-card-text>
      <v-card-actions>
        <v-autocomplete
          label="Select partner"
          v-model="partner"
          :items="partners"
          return-object
          class="mx-4"
          flat
          hide-no-data
          hide-details
          dense
          outlined
          clearable
          clear-icon="$vuetify.icons.delete"
        ></v-autocomplete>
        <v-autocomplete
            label="Select credential"
            v-model="credDef"
            :items="credDefs"
            return-object
            class="mx-4"
            flat
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
            <v-btn
                v-bind="attrs"
                v-on="on"
                color="primary"
                :disabled="issueCredentialDisabled"
            >Issue Credential</v-btn>
          </template>
          <IssueCredential
              :credDefId="credDefId"
              :partnerId="partnerId"
              :credDefList="credDefs"
              :partnerList="partners"
              @success="credentialIssued"
              @cancelled="issueCredentialDialog = false">
          </IssueCredential>
        </v-dialog>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
  import { EventBus } from "@/main";
  import {issuerService, partnerService} from "@/services";
  import CredExList from "@/components/CredExList";
  import IssueCredential from "@/components/IssueCredential";
  import * as textUtils from "@/utils/textUtils";

  export default {
    name: "CredentialManagement",
    components: {
      IssueCredential,
      CredExList
    },
    created() {
      EventBus.$emit("title", "Credential Management");
      this.load();
    },
    data: () => {
      return {
        issuedCredentials: [],
        partners: [],
        partner: {},
        partnerId: "",
        credDefs: [],
        credDef: {},
        credDefId: "",
        issueCredentialDisabled: true,
        issueCredentialDialog: false
      };
    },
    computed: { },
    watch: {
      partner (val) {
        this.issueCredentialDisabled = (!val || !val.id) || (!this.credDef || !this.credDef.id);
        this.partnerId = val ? val.id : "";
      },
      credDef (val) {
        this.issueCredentialDisabled = (!val || !val.id) || (!this.partner || !this.partner.id);
        this.credDefId = val ? val.id : "";
      },
    },
    methods: {
      async load() {
        this.isLoading = true;
        this.issuedCredentials = [];
        this.partners = [];
        this.partner = {};
        this.credDefs = [];
        this.credDef = {};

        // get partner list
        const presp = await partnerService.listPartners();
        if (presp.status === 200) {
          this.partners = presp.data.map((p) => {
            return { value: p.id, text: p.alias, ...p };
          });
        }

        // get list of schema/creddefs
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

        const iresp = await issuerService.listCredentialExchangesAsIssuer();
        if (iresp.status === 200) {
          this.issuedCredentials = iresp.data;
        }
        this.isLoading = false;
      },
      credentialIssued() {
        this.issueCredentialDialog = false;
        this.load();
      }
    },
  };
</script>
