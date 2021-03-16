<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container justify-center>
    <v-card v-if="!isLoading" class="mx-auto">
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>mdi-chevron-left</v-icon>
        </v-btn>
        <span>{{ data.label }}</span>
        <v-layout align-end justify-end>
          <!-- <v-btn depressed icon @click="isUpdatingName = !isUpdatingName">
                    <v-icon dark>mdi-pencil</v-icon>
                </v-btn> -->
          <v-btn
            depressed
            color="red"
            icon
            :disabled="data.isReadOnly"
            @click="deleteSchema"
          >
            <v-icon dark>mdi-delete</v-icon>
          </v-btn>
        </v-layout>
      </v-card-title>
      <v-container>
        <v-list-item class="mt-4">
          <v-list-item-title
            class="grey--text text--darken-2 font-weight-medium"
          >
            Schema ID:
          </v-list-item-title>
          <v-list-item-subtitle>
            {{ data.schemaId }}
          </v-list-item-subtitle>
        </v-list-item>

        <h4 class="my-4 grey--text text--darken-3">Schema Attributes</h4>

        <v-list-item
          v-for="attribute in data.schemaAttributeNames"
          :key="attribute.id"
        >
          <p class="grey--text text--darken-2 font-weight-medium">
            {{ attribute }}
          </p>
        </v-list-item>

        <h4 class="my-4 grey--text text--darken-3">Trusted Issuers</h4>

        <v-row v-for="(entry, index) in trustedIssuers" v-bind:key="index">
          <v-col cols="4" class="py-0">
            <v-text-field
              label="DID"
              :disabled="entry.readOnly || !entry.isEdit"
              v-model="entry.issuerDid"
              outlined
              dense
            ></v-text-field>
          </v-col>
          <v-col class="py-0">
            <v-text-field
              label="Name"
              :disabled="entry.readOnly || !entry.isEdit"
              v-model="entry.label"
              outlined
              dense
            ></v-text-field>
          </v-col>
          <v-col cols="3" class="py-0">
            <v-btn
              v-if="!entry.readOnly && !entry.isEdit"
              :disabled="isEdit"
              color="primary"
              text
              @click="editTrustedIssuer(index)"
              >Edit</v-btn
            >

            <v-btn
              v-if="!entry.readOnly && entry.isEdit"
              :loading="isBusy"
              color="primary"
              text
              @click="saveTrustedIssuer(entry)"
              >Save</v-btn
            >

            <v-btn
              v-if="!entry.readOnly && entry.isEdit"
              color="secondary"
              text
              @click="cancelEditTrustedIssuer(index)"
              >Cancel</v-btn
            >

            <v-btn
              icon
              v-if="!entry.readOnly && !entry.isEdit"
              @click="deleteTrustedIssuer(index)"
            >
              <v-icon color="error">mdi-delete</v-icon>
            </v-btn>
          </v-col>
        </v-row>
        <v-btn :disabled="isEdit" color="primary" text @click="addTrustedIssuer"
          >Add</v-btn
        >
      </v-container>
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-btn color="primary" text @click="cancel()">Close</v-btn>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
export default {
  name: "Schema",
  props: {
    id: String, //schema ID
    schema: Object,
  },
  created() {
    EventBus.$emit("title", "Schema");
    console.log("SCHEMA", this.schema);
    if (this.schema) {
      this.isLoading = false;
      this.data = this.schema;
    } else {
      this.fetch();
    }
  },
  data: () => {
    return {
      data: [],
      isEdit: false,
      editingTrustedIssuer: {},
      isLoading: true,
      isBusy: false,
      trustedIssuers: [
        {
          issuerDid: "",
          label: "",
        },
      ],
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
          if ({}.hasOwnProperty.call(result, "data")) {
            this.data = result.data;
            // Init restrictions
            if ({}.hasOwnProperty.call(this.data, "trustedIussuers")) {
              this.trustedIssuers = this.data.trustedIssuers;
            }
            this.isLoading = false;
          }
        })
        .catch((e) => {
          this.isLoading = false;
          if (e.response.status === 404) {
            this.data = [];
          } else {
            console.error(e);
            EventBus.$emit("error", e);
          }
        });
    },
    addTrustedIssuer() {
      this.isEdit = true;
      this.trustedIssuers.push({
        issuerDid: "",
        label: "",
        isEdit: true,
      });
    },
    editTrustedIssuer(index) {
      this.editingTrustedIssuer = Object.assign({}, this.trustedIssuers[index]);
      this.isEdit = true;
      this.trustedIssuers[index].isEdit = true;
    },
    cancelEditTrustedIssuer(index) {
      this.trustedIssuers[index] = this.editingTrustedIssuer;
      this.isEdit = false;
      this.trustedIssuers[index].isEdit = false;
    },
    deleteTrustedIssuer(index) {
      let trustedIssuer = this.trustedIssuers[index];
      this.$axios
        .delete(
          `${this.$apiBaseUrl}/admin/schema/${this.id}/trustedIssuer/${trustedIssuer.id}`
        )
        .then((result) => {
          console.log(result);
          this.trustedIssuers.splice(index, 1);
        })
        .catch((e) => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },

    saveTrustedIssuer(trustedIssuer) {
      // delete trustedIssuer.isEdit;
      if (trustedIssuer.id) {
        this.updateTrustedIssuer(trustedIssuer);
      } else {
        this.createNewTrustedIssuer(trustedIssuer);
      }
    },

    createNewTrustedIssuer(trustedIssuer) {
      this.isBusy = true;

      this.$axios
        .post(
          `${this.$apiBaseUrl}/admin/schema/${this.id}/trustedIssuer`,
          trustedIssuer
        )
        .then((result) => {
          console.log(result);
          this.isBusy = false;

          if (result.status === 200) {
            (this.isEdit = false),
              (trustedIssuer.isEdit = false),
              EventBus.$emit("success", "New trusted issuer added");
          }
        })
        .catch((e) => {
          this.isBusy = false;
          console.error(e);
          EventBus.$emit("error", e);
        });
    },

    updateTrustedIssuer(trustedIssuer) {
      this.isBusy = true;

      this.$axios
        .put(
          `${this.$apiBaseUrl}/admin/schema/${this.id}/trustedIssuer/${this.trustedIssuer.id}`,
          trustedIssuer
        )
        .then((result) => {
          console.log(result);
          this.isBusy = false;
          trustedIssuer.isEdit = false;

          if (result.status === 201) {
            EventBus.$emit("success", "Trusted issuer updated");
          }
        })
        .catch((e) => {
          this.isBusy = false;
          trustedIssuer.isEdit = false;
          console.error(e);
          EventBus.$emit("error", e);
        });
    },

    deleteSchema() {
      this.$axios
        .delete(`${this.$apiBaseUrl}/admin/schema/${this.id}`)
        .then((result) => {
          console.log(result);
          if (result.status === 200) {
            EventBus.$emit("success", "Schema deleted");
            this.$router.push({
              name: "SchemaSettings",
            });
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

<style scoped>
.bg-light {
  background-color: #fafafa;
}

.bg-light-2 {
  background-color: #ececec;
}
</style>
