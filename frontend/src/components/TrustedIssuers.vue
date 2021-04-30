<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <div>
    <h4 class="my-4 grey--text text--darken-3">Trusted Issuers</h4>

    <v-row v-for="(entry, index) in trustedIssuers" v-bind:key="index">
      <v-col cols="4" class="py-0">
        <v-text-field
          label="DID"
          :disabled="entry.isReadOnly || !entry.isEdit"
          v-model="entry.issuerDid"
          outlined
          dense
        ></v-text-field>
      </v-col>
      <v-col class="py-0">
        <v-text-field
          label="Name"
          :disabled="entry.isReadOnly || !entry.isEdit"
          v-model="entry.label"
          outlined
          dense
        ></v-text-field>
      </v-col>
      <v-col cols="3" class="py-0">
        <v-btn
          v-if="!entry.isReadOnly && !entry.isEdit"
          :disabled="isEdit"
          color="primary"
          text
          @click="editTrustedIssuer(index)"
          >Edit</v-btn
        >

        <v-btn
          v-if="!entry.isReadOnly && entry.isEdit"
          :loading="isBusy"
          color="primary"
          text
          @click="saveTrustedIssuer(entry)"
          >Save</v-btn
        >

        <v-btn
          v-if="!entry.isReadOnly && entry.isEdit"
          color="secondary"
          text
          @click="cancelEditTrustedIssuer(index)"
          >Cancel</v-btn
        >

        <v-btn
          icon
          v-if="!entry.isReadOnly && !entry.isEdit"
          @click="deleteTrustedIssuer(index)"
        >
          <v-icon color="error">$vuetify.icons.delete</v-icon>
        </v-btn>
      </v-col>
    </v-row>
    <v-btn :disabled="isEdit" color="primary" text @click="addTrustedIssuer"
      >Add trusted issuer</v-btn
    >
  </div>
</template>

<script>
import { EventBus } from "../main";
export default {
  props: {
    id: String, //schema ID
    trustedIssuers: {
      type: Array,
      default: function () {
        return [];
      },
    },
  },
  created() {},
  mounted() {},

  data: () => {
    return {
      isEdit: false,
      editingTrustedIssuer: null,
      isBusy: false,
    };
  },
  computed: {},
  methods: {
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
      this.isEdit = false;
      if (!this.editingTrustedIssuer) {
        this.trustedIssuers.splice(index, 1);
      } else {
        this.trustedIssuers[index] = this.editingTrustedIssuer;
        this.trustedIssuers[index].isEdit = false;
      }
      this.editingTrustedIssuer = null;
    },
    deleteTrustedIssuer(index) {
      let trustedIssuer = this.trustedIssuers[index];
      if (trustedIssuer.id) {
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
      } else {
        this.trustedIssuers.splice(index, 1);
      }
    },

    saveTrustedIssuer(trustedIssuer) {
      if (trustedIssuer.id) {
        this.updateTrustedIssuer(trustedIssuer);
      } else if (this.id) {
        this.createNewTrustedIssuer(trustedIssuer);
      } else {
        this.isEdit = false;
        trustedIssuer.isEdit = false;
      }
      this.editingTrustedIssuer = null;
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
            this.isEdit = false;
            trustedIssuer.isEdit = false;
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
          `${this.$apiBaseUrl}/admin/schema/${this.id}/trustedIssuer/${trustedIssuer.id}`,
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
  },
};
</script>
