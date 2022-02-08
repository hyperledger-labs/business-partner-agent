<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card v-if="isReady" class="mx-auto">
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        <div v-if="credential.type === CredentialTypes.UNKNOWN.type">
          {{ credential.credentialDefinitionId | credentialTag }}
        </div>
        <div v-else>
          {{ credential.typeLabel | capitalize }}
        </div>
        <v-layout align-end justify-end>
          <v-btn depressed color="red" icon @click="deleteCredential()">
            <v-icon dark>$vuetify.icons.delete</v-icon>
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
            <v-list-item-title>{{
              $t("view.wallet.visibility.title")
            }}</v-list-item-title>
            <v-list-item-subtitle>{{
              $t("view.wallet.visibility.subtitle")
            }}</v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action>
            <v-switch v-model="isPublic"></v-switch>
          </v-list-item-action>
        </v-list-item>
        <v-divider></v-divider>
      </v-card-text>
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-bpa-button color="secondary" @click="cancel()">{{
            $t("button.cancel")
          }}</v-bpa-button>
          <v-bpa-button
            :loading="this.isBusy"
            color="primary"
            @click="saveChanges()"
            >{{ $t("button.save") }}</v-bpa-button
          >
        </v-layout>
      </v-card-actions>
      <v-expansion-panels v-if="expertMode" accordion flat>
        <v-expansion-panel>
          <v-expansion-panel-header
            class="grey--text text--darken-2 font-weight-medium bg-light"
            >{{ $t("showRawData") }}</v-expansion-panel-header
          >
          <v-expansion-panel-content class="bg-light">
            <vue-json-pretty :data="credential"></vue-json-pretty>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import Cred from "@/components/Credential.vue";
import { CredentialTypes } from "@/constants";
import VBpaButton from "@/components/BpaButton";

export default {
  name: "Credential",
  props: {
    id: String,
    type: String,
  },
  created() {
    EventBus.$emit("title", this.$t("view.credential.title"));
    this.getCredential();
    this.$store.commit("credentialNotificationSeen", { id: this.id });
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
      console.log("Get Credential ID:", this.id);
      this.$axios
        .get(`${this.$apiBaseUrl}/wallet/credential/${this.id}`)
        .then((result) => {
          if (Object.prototype.hasOwnProperty.call(result, "data")) {
            this.credential = result.data;
            this.credential.label = Object.prototype.hasOwnProperty.call(
              result.data,
              "label"
            )
              ? result.data.label
              : "";
            this.isPublic = this.credential.isPublic;
            this.isReady = true;
            this.intDoc = { ...this.credential };
          }
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
    saveChanges() {
      const requests = [];
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
            const allResponsesTrue = responses.every((response) => {
              console.log(response);
              return response.status === 200;
            });
            if (allResponsesTrue) {
              EventBus.$emit(
                "success",
                this.$t("view.credential.eventSuccessUpdate")
              );
              this.$router.push({
                name: "Wallet",
              });
            }
          })
        )
        .catch((error) => {
          for (const errorElement of error) {
            console.error(errorElement);
          }
          // react on errors.
          EventBus.$emit("errors", error);
        });
    },
    deleteCredential() {
      this.$axios
        .delete(`${this.$apiBaseUrl}/wallet/credential/${this.id}`)
        .then((result) => {
          console.log(result);
          if (result.status === 200) {
            EventBus.$emit(
              "success",
              this.$t("view.credential.eventSuccessDelete")
            );
            this.$router.push({
              name: "Wallet",
            });
          }
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
    cancel() {
      this.$router.push({
        name: "Wallet",
      });
    },
    fieldModified(keyValue) {
      this.docChanged = Object.keys(this.intDoc).find((key) => {
        return this.credential[key] !== this.intDoc[key];
      });
      if (this.docChanged) {
        this.credential[keyValue.key] = keyValue.value;
      }
    },
  },
  components: {
    VBpaButton,
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
