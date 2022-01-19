<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container justify-center>
    <v-card v-if="!isLoading" class="mx-auto">
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        <span>{{ data.label }}</span>
        <v-layout align-end justify-end>
          <v-btn
            depressed
            color="red"
            icon
            :disabled="data.isReadOnly"
            @click="deleteSchema"
          >
            <v-icon dark>$vuetify.icons.delete</v-icon>
          </v-btn>
        </v-layout>
      </v-card-title>
      <v-container>
        <v-list-item class="mt-4">
          <v-list-item-title
            class="grey--text text--darken-2 font-weight-medium"
          >
            {{ $t("view.schema.id") }}:
          </v-list-item-title>
          <v-list-item-subtitle>
            {{ data.schemaId }}
          </v-list-item-subtitle>
        </v-list-item>

        <h4 class="my-4 grey--text text--darken-3">
          {{ $t("view.schema.attributes") }}
        </h4>

        <v-list-item
          v-for="attribute in data.schemaAttributeNames"
          :key="attribute.id"
        >
          <p class="grey--text text--darken-2 font-weight-medium">
            {{ attribute }}
          </p>
        </v-list-item>

        <trusted-issuers :id="id" :trustedIssuers="trustedIssuers" />
      </v-container>
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-bpa-button color="primary" :to="{ name: 'SchemaSettings' }">{{
            $t("button.close")
          }}</v-bpa-button>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import TrustedIssuers from "../components/TrustedIssuers.vue";
import VBpaButton from "@/components/BpaButton";
export default {
  name: "Schema",
  props: {
    id: String, //schema ID
    schema: Object,
  },
  components: {
    VBpaButton,
    TrustedIssuers,
  },
  mounted() {
    EventBus.$emit("title", this.$t("view.schema.title"));
    console.log("SCHEMA", this.schema);
    this.fetch();
  },
  data: () => {
    return {
      data: [],
      isLoading: true,
      trustedIssuers: [],
    };
  },
  computed: {},
  methods: {
    fetch() {
      this.isLoading = true;
      this.$axios
        .get(`${this.$apiBaseUrl}/admin/schema/${this.id}`)
        .then((result) => {
          console.log(result);
          if (Object.prototype.hasOwnProperty.call(result, "data")) {
            this.data = result.data;
            // Init trusted issuers
            if (
              Object.prototype.hasOwnProperty.call(this.data, "trustedIssuer")
            ) {
              this.trustedIssuers = this.data.trustedIssuer;
            }
            this.isLoading = false;
          }
        })
        .catch((error) => {
          this.isLoading = false;
          if (error.response.status === 404) {
            this.data = [];
          } else {
            EventBus.$emit("error", this.$axiosErrorMessage(error));
          }
        });
    },
    deleteSchema() {
      this.$axios
        .delete(`${this.$apiBaseUrl}/admin/schema/${this.id}`)
        .then((result) => {
          console.log(result);
          if (result.status === 200) {
            EventBus.$emit(
              "success",
              this.$t("view.schema.eventSuccessDelete")
            );
            this.$router.push({
              name: "SchemaSettings",
            });
          }
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
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
