<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-card class="mx-auto">
    <v-card-title class="bg-light">
      <v-btn depressed color="secondary" icon @click="$router.go(-1)">
        <v-icon dark>mdi-chevron-left</v-icon>
      </v-btn>
      Create a Presentation Request
    </v-card-title>
    <v-card-text>
      <h4 class="pt-4">Select a credential type to request</h4>
      <v-data-table
        hide-default-footer
        v-model="selected"
        :show-select="true"
        single-select
        :headers="headers"
        :items="templates[this.$config.ledger]"
        item-key="credentialDefinitionId"
      >
        <template v-slot:[`item.credentialDefinitionId`]="{ item }">
          <span v-if="item.label"> {{ item.label }} </span>
          <span v-else>
            {{ item.credentialDefinitionId | credentialTag }}
          </span>
        </template>
      </v-data-table>
      <div v-if="expertMode">
        <h4 class="pt-4">Or enter a custom Credential Definition ID</h4>
        <v-text-field
          label="Credential Definition ID"
          placeholder=""
          v-model="credDefId"
          outlined
          dense
        >
        </v-text-field>
      </div>
    </v-card-text>

    <v-card-actions>
      <v-layout align-end justify-end>
        <v-btn color="secondary" text @click="cancel()">Cancel</v-btn>
        <v-btn
          :loading="this.isBusy"
          color="primary"
          text
          @click="submitRequest()"
          >Submit</v-btn
        >
      </v-layout>
    </v-card-actions>
  </v-card>
</template>

<script>
import { EventBus } from "../main";

import { CredentialTypes } from "../constants";

export default {
  name: "RequestPresentation",
  components: {},
  props: {
    id: String, //partner ID
  },
  created() {
    EventBus.$emit("title", "Request Presentation");
    console.log(this.$config);
  },
  data: () => {
    return {
      isBusy: false,
      credDefId: "",
      selected: [],
      CredentialTypes: CredentialTypes,
      headers: [
        {
          text: "Type",
          value: "credentialDefinitionId",
        },
        {
          text: "Issuer",
          value: "issuer",
        },
      ],
      templates: {
        iil: [
          // {
          //   credentialDefinitionId: {
          //     iiL: "nJvGcV7hBSLRSUvwGk2hT:3:CL:734:IATF Certificate",
          //     idu: ""
          //   }
          //     ,
          //   issuer: "IATF Proxy Issuer",
          // },
          {
            credentialDefinitionId:
              "5mwQSWnRePrZ3oF67C4KqD:3:CL:1077:commercial register entry",
            label: "Commercial Registry Entry",
            issuer: "Commercial Registry",
          },
          // {
          //     credentialDefinitionId: "8faozNpSjFfPJXYtgcPtmJ:3:CL:1041:Commercial Registry Entry (Open Corporates)",
          //     issuer: "Commercial Registry"
          // },
          {
            credentialDefinitionId:
              "M6Mbe3qx7vB4wpZF4sBRjt:3:CL:571:bank_account_no_revoc",
            label: "Bank Account",
            issuer: "Bank",
          },
        ],
        idu: [
          {
            credentialDefinitionId:
              "R6WR6n7CQVDjvvmwofHK6S:3:CL:109:Commercial Registry Entry",
            label: "Commercial Registry Entry",
            issuer: "Trust Service Provider",
          },
          {
            credentialDefinitionId:
              "UmZ25DANwS6ngGWB4ye4tN:3:CL:104:Bank Account",
            label: "Bank Account",
            issuer: "Bank",
          },
          {
            credentialDefinitionId: "3QowxFtwciWceMFr7WbwnM:3:CL:104:BankCard",
            label: "Bank Account",
            issuer: "CommerzBank",
          },
        ],
      },
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
  },
  methods: {
    submitRequest() {
      this.isBusy = true;
      console.log(this.selected);
      if (this.selected.length === 1 || this.credDefId.length > 0) {
        let credDefId =
          this.selected.length === 1
            ? this.selected[0].credentialDefinitionId
            : this.credDefId;

        this.$axios
          .post(`${this.$apiBaseUrl}/partners/${this.id}/proof-request`, {
            credentialDefinitionId: credDefId,
          })
          .then((res) => {
            console.log(res);
            this.isBusy = false;
            EventBus.$emit("success", "Presentation request sent");
            this.$router.go(-1);
          })
          .catch((e) => {
            this.isBusy = false;
            console.error(e);
            EventBus.$emit("error", e);
          });
      } else {
        this.isBusy = false;
        EventBus.$emit("error", "No credential type selected");
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
