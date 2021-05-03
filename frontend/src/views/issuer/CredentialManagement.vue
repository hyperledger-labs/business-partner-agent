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
            v-bind:headers="issuedHeaders"
        ></CredExList>
      </v-card-text>
      <v-card-actions>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
  import { EventBus } from "@/main";
  import {issuerService} from "@/services";
  import CredExList from "@/components/CredExList";

  export default {
    name: "CredentialManagement",
    components: {
      CredExList
    },
    created() {
      EventBus.$emit("title", "Credential Management");
      this.getIssuedCredentials();
    },
    data: () => {
      return {
        loadingCredentials: false,
        issuedCredentials: [],
      };
    },
    computed: {
      issuedHeaders() {
        return [
          {
            text: "Type",
            value: "displayText",
          },
          {
            text: "Issued To",
            value: "partner.alias",
          },
          {
            text: "Updated at",
            value: "updatedAt",
          },
          {
            text: "State",
            value: "state",
          },
        ]
      }
    },
    methods: {
      getIssuedCredentials() {
        console.log("Getting issued credential records...");
        issuerService
          .listCredentialExchangesAsIssuer()
          .then((result) => {
            if ({}.hasOwnProperty.call(result, "data")) {
              let data = result.data;
              this.issuedCredentials = data;
            }
          })
          .catch((e) => {
            console.error(e);
            EventBus.$emit("error", e);
          });
      },
    },
  };
</script>
