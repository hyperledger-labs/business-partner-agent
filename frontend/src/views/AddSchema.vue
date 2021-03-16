<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card max-width="600" class="mx-auto">
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>mdi-chevron-left</v-icon>
        </v-btn>
        <span>Add Schema</span>
      </v-card-title>

      <v-list-item>
        <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
          Schema Name:
        </v-list-item-title>
        <v-list-item-subtitle>
          <v-text-field
            class="mt-6"
            placeholder="Name"
            v-model="schema.label"
            :rules="[rules.required]"
            outlined
            dense
            required
          >
          </v-text-field>
        </v-list-item-subtitle>
      </v-list-item>
      <v-list-item>
        <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
          Schema ID:
        </v-list-item-title>
        <v-list-item-subtitle>
          <v-text-field
            class="mt-6"
            placeholder="Schema ID"
            v-model="schema.schemaId"
            :rules="[rules.required]"
            outlined
            dense
            required
          >
          </v-text-field>
        </v-list-item-subtitle>
      </v-list-item>

      <v-card-text>
        <trusted-issuer ref="trustedIssuers" />
      </v-card-text>

      <v-card-actions>
        <v-layout justify-end>
          <v-btn
            :loading="this.isBusyAddSchema"
            :disabled="fieldsEmpty"
            color="primary"
            @click="addSchema"
          >
            Submit
          </v-btn>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
import TrustedIssuer from "../components/TrustedIssuers.vue";
export default {
  name: "AddSchema",
  components: {
    TrustedIssuer,
  },
  created: () => {},

  data: () => {
    return {
      schema: {
        label: "",
        schemaId: "",
      },
      isBusyAddSchema: false,
      rules: {
        required: (value) => !!value || "Can't be empty",
      },
    };
  },
  computed: {
    fieldsEmpty() {
      return (
        this.schema.label.length === 0 || this.schema.schemaId.length === 0
      );
    },
  },
  methods: {
    addSchema() {
      this.isBusyAddSchema = true;
      let trustedIssuers = this.$refs.trustedIssuers.$props.trustedIssuers;
      console.log(trustedIssuers);
      if (trustedIssuers.length > 0) {
        trustedIssuers.forEach((entry) => {
          delete entry.isEdit;
        });
        this.schema.trustedIssuer = trustedIssuers;
      }
      console.log(this.schema);

      this.$axios
        .post(`${this.$apiBaseUrl}/admin/schema`, this.schema)
        .then((result) => {
          console.log(result);
          this.isBusyAddSchema = false;

          if (result.status === 200 || result.status === 200) {
            EventBus.$emit("success", "Schema added successfully");
            this.$router.push({ name: "SchemaSettings" });
          }
        })
        .catch((e) => {
          this.isBusyAddSchema = false;
          if (e.response.status === 400) {
            EventBus.$emit("error", "Schema already exists");
          } else {
            console.error(e);
            EventBus.$emit("error", e);
          }
        });
    },
  },
};
</script>
