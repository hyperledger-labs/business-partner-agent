<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card v-if="isReady" class="mx-auto">
      <v-card-title class="bg-light">
        <v-btn
          depressed
          color="secondary"
          icon
          @click="$router.push({ name: 'Wallet' })"
        >
          <v-icon dark>mdi-chevron-left</v-icon>
        </v-btn>
        <div v-if="credential.type === CredentialTypes.OTHER.name">
          {{ credential.credentialDefinitionId | credentialTag }}
        </div>
        <div v-else>{{ credential.type | credentialLabel }}</div>
        <v-layout align-end justify-end>
          <v-btn depressed color="red" icon @click="deleteCredential()">
            <v-icon dark>mdi-delete</v-icon>
          </v-btn>
        </v-layout>
      </v-card-title>
      <v-card-text>
        <Cred
          v-bind:document="credential"
          isReadOnly
          @doc-field-changed="fieldModified"
        >
        </Cred>
        <v-divider></v-divider>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>Public Profile</v-list-item-title>
            <v-list-item-subtitle
              >Visible in Public Profile</v-list-item-subtitle
            >
          </v-list-item-content>
          <v-list-item-action>
            <v-switch
              :disabled="credential.type === CredentialTypes.OTHER.name"
              v-model="isPublic"
            ></v-switch>
          </v-list-item-action>
        </v-list-item>
        <v-divider></v-divider>
      </v-card-text>
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-btn color="secondary" text @click="cancel()">Cancel</v-btn>
          <v-btn
            :loading="this.isBusy"
            color="primary"
            text
            @click="saveChanges()"
            >Save</v-btn
          >
        </v-layout>
      </v-card-actions>
      <v-expansion-panels v-if="expertMode" accordion flat>
        <v-expansion-panel>
          <v-expansion-panel-header
            class="grey--text text--darken-2 font-weight-medium bg-light"
            >Show raw data</v-expansion-panel-header
          >
          <v-expansion-panel-content class="bg-light">
            <vue-json-pretty :data="credential"></vue-json-pretty>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
import Cred from "@/components/Credential";
import { CredentialTypes } from "../constants";

export default {
  name: "Credential",
  props: {
    id: String,
    type: String,
  },
  created() {
    EventBus.$emit("title", "Credential");
    this.getCredential();

    this.$store.commit("credentialSeen", { id: this.id });
  },
  data: () => {
    return {
      credential: {},
      intDocument: {},
      isBusy: false,
      isReady: false,
      docChanged: false,
      CredentialTypes: CredentialTypes,
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
  },
  methods: {
    getCredential() {
      console.log(this.id);
      this.$axios
        .get(`${this.$apiBaseUrl}/wallet/credential/${this.id}`)
        .then((result) => {
          if ({}.hasOwnProperty.call(result, "data")) {
            this.credential = result.data;
            this.isPublic = this.credential.isPublic;
            this.isReady = true;
            this.intDoc = { ...this.credential };
          }
        })
        .catch((e) => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    saveChanges() {
      var requests = [];
      if (this.credential.isPublic !== this.isPublic) {
        requests.push(
          this.$axios.put(
            `${this.$apiBaseUrl}/wallet/credential/${this.id}/toggle-visibility`
          )
        );
      }

      if (this.docChanged) {
        requests.push(
          this.$axios.put(`${this.$apiBaseUrl}/wallet/credential/${this.id}`, {
            label: this.credential.label,
          })
        );
      }

      this.$axios
        .all(requests)
        .then(
          this.$axios.spread((...responses) => {
            var allResponsesTrue = responses.every((response) => {
              console.log(response);
              return response.status === 200;
            });
            if (allResponsesTrue) {
              EventBus.$emit("success", "Credential updated");
              this.$router.push({
                name: "Wallet",
              });
            }
          })
        )
        .catch((errors) => {
          errors.forEach((error) => {
            console.error(error);
          });
          // react on errors.
          EventBus.$emit("errors", errors);
        });
    },
    deleteCredential() {
      this.$axios
        .delete(`${this.$apiBaseUrl}/wallet/credential/${this.id}`)
        .then((result) => {
          console.log(result);
          if (result.status === 200) {
            EventBus.$emit("success", "Credential deleted");
            this.$router.push({
              name: "Wallet",
            });
          }
        })
        .catch((e) => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    cancel() {
      this.$router.push({
        name: "Wallet",
      });
    },
    fieldModified(keyValue) {
      const isModified = Object.keys(this.intDoc).find((key) => {
        return this.credential[key] != this.intDoc[key];
      })
        ? true
        : false;
      this.docChanged = isModified;
      if (this.docChanged) {
        this.credential[keyValue.key] = keyValue.value;
      }
    },
  },
  components: {
    Cred,
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
