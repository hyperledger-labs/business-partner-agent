<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-row v-for="(entry, index) in items" v-bind:key="index">
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
          icon
          @click="editTrustedIssuer(index)"
          ><v-icon>$vuetify.icons.pencil</v-icon></v-btn
        >

        <v-btn
          v-if="!entry.isReadOnly && entry.isEdit"
          :loading="isBusy"
          color="primary"
          icon
          @click="saveTrustedIssuer(entry)"
          ><v-icon>$vuetify.icons.save</v-icon></v-btn
        >

        <v-btn
          v-if="!entry.isReadOnly && entry.isEdit"
          color="error"
          icon
          @click="cancelEditTrustedIssuer(index)"
          ><v-icon>$vuetify.icons.cancel</v-icon></v-btn
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
    <v-row>
      <v-bpa-button :disabled="isEdit" color="secondary" @click="addTrustedIssuer"
        >Add trusted issuer</v-bpa-button
      >
    </v-row>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
import VBpaButton from "@/components/BpaButton";
export default {
  components: {VBpaButton},
  props: {
    schema: {
      type: Object,
      default: () => {},
    },
    trustedIssuers: {
      type: Array,
      default: function () {
        return [];
      },
    },
  },
  watch: {
    schema() {
      this.isEdit = false;
      this.editingItem = false;
    },
    trustedIssuers(val) {
      this.items = Array.from(val);
      this.isEdit = false;
      this.editingTrustedIssuer = null;
    },
  },
  created() {},
  mounted() {
    this.items = Array.from(this.trustedIssuers);
  },
  data: () => {
    return {
      items: [],
      isEdit: false,
      editingTrustedIssuer: null,
      isBusy: false,
    };
  },
  computed: {},
  methods: {
    addTrustedIssuer() {
      this.isEdit = true;
      this.items.push({
        issuerDid: "",
        label: "",
        isEdit: true,
      });
    },
    editTrustedIssuer(index) {
      this.editingTrustedIssuer = Object.assign({}, this.items[index]);
      this.isEdit = true;
      this.items[index].isEdit = true;
    },
    cancelEditTrustedIssuer(index) {
      this.isEdit = false;
      if (!this.editingTrustedIssuer) {
        this.items.splice(index, 1);
      } else {
        this.items[index] = this.editingTrustedIssuer;
        this.items[index].isEdit = false;
      }
      this.editingTrustedIssuer = null;
    },
    deleteTrustedIssuer(index) {
      let trustedIssuer = this.items[index];
      if (trustedIssuer.id) {
        this.$axios
          .delete(
            `${this.$apiBaseUrl}/admin/schema/${this.schema.id}/trustedIssuer/${trustedIssuer.id}`
          )
          .then((result) => {
            console.log(result);
            this.items.splice(index, 1);
            this.$emit("changed");
          })
          .catch((e) => {
            console.error(e);
            EventBus.$emit("error", e);
          });
      } else {
        this.items.splice(index, 1);
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
          `${this.$apiBaseUrl}/admin/schema/${this.schema.id}/trustedIssuer`,
          trustedIssuer
        )
        .then((result) => {
          console.log(result);
          this.isBusy = false;

          if (result.status === 200) {
            this.isEdit = false;
            trustedIssuer.isEdit = false;
            EventBus.$emit("success", "New trusted issuer added");
            this.$emit("changed");
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
          `${this.$apiBaseUrl}/admin/schema/${this.schema.id}/trustedIssuer/${trustedIssuer.id}`,
          trustedIssuer
        )
        .then((result) => {
          console.log(result);
          this.isBusy = false;
          trustedIssuer.isEdit = false;

          if (result.status === 201) {
            EventBus.$emit("success", "Trusted issuer updated");
            this.$emit("changed");
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
