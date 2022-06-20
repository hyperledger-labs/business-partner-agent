<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-row v-for="(entry, index) in items" v-bind:key="index">
      <v-col cols="4" class="py-0">
        <v-text-field
          :label="$t('component.trustedIssuers.labelDid')"
          :disabled="entry.isReadOnly || !isNew(entry)"
          v-model="entry.issuerDid"
          outlined
          dense
          @change="onDidChanged"
        ></v-text-field>
      </v-col>
      <v-col class="py-0">
        <v-text-field
          :label="$t('component.trustedIssuers.labelName')"
          :disabled="entry.isReadOnly || !entry.isEdit"
          v-model="entry.label"
          outlined
          dense
          @change="onNameChanged"
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
      <v-bpa-button
        :disabled="isEdit"
        color="secondary"
        @click="addTrustedIssuer"
        >{{ $t("component.trustedIssuers.addTrustedIssuer") }}</v-bpa-button
      >
    </v-row>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import VBpaButton from "@/components/BpaButton";
import { adminService, TrustedIssuer } from "@/services";
export default {
  components: { VBpaButton },
  props: {
    schema: {
      type: Object,
    },
    trustedIssuers: {
      type: Array,
      default: function () {
        return new Array<TrustedIssuer>();
      },
    },
    reset: {
      type: Boolean,
      default: () => false,
    },
  },
  watch: {
    schema() {
      this.isEdit = false;
      this.editingItem = false;
    },
    trustedIssuers(value: (TrustedIssuer & { isEdit: boolean })[]) {
      this.items = [...value];
      this.isEdit = false;
      this.editingTrustedIssuer = undefined;
    },
    reset(newValue: boolean, oldValue: boolean) {
      // use this to reset the form, remove any outstanding items that are not saved.
      if (newValue !== oldValue) {
        this.items = [...this.trustedIssuers];
        this.isEdit = false;
        this.editingTrustedIssuer = undefined;
      }
    },
  },
  mounted() {
    this.items = [...this.trustedIssuers];
  },
  data: () => {
    return {
      items: new Array<TrustedIssuer & { isEdit: boolean }>(),
      isEdit: false,
      isDirty: false,
      editingTrustedIssuer: {} as TrustedIssuer & { isEdit: boolean },
      isBusy: false,
    };
  },
  computed: {},
  methods: {
    isNew(entry: TrustedIssuer & { isEdit: boolean }) {
      return !Object.prototype.hasOwnProperty.call(entry, "id");
    },
    onDidChanged() {
      this.isDirty = true;
    },
    onNameChanged() {
      this.isDirty = true;
    },
    addTrustedIssuer() {
      this.isEdit = true;
      this.items.push({
        issuerDid: "",
        label: "",
        isEdit: true,
      });
    },
    editTrustedIssuer(index: number) {
      this.editingTrustedIssuer = Object.assign({}, this.items[index]);
      this.isEdit = true;
      this.items[index].isEdit = true;
    },
    cancelEditTrustedIssuer(index: number) {
      this.isEdit = false;
      if (!this.editingTrustedIssuer) {
        this.items.splice(index, 1);
      } else {
        this.items[index] = this.editingTrustedIssuer;
        this.items[index].isEdit = false;
      }
      this.editingTrustedIssuer = undefined;
    },
    deleteTrustedIssuer(index: number) {
      let trustedIssuer = this.items[index];
      if (trustedIssuer.id) {
        adminService
          .deleteTrustedIssuer(this.schema.id, trustedIssuer.id)
          .then((result) => {
            if (result.status === 200) {
              this.items.splice(index, 1);
              this.$emit("changed");
            }
          })
          .catch((error) => {
            EventBus.$emit("error", this.$axiosErrorMessage(error));
          });
      } else {
        this.items.splice(index, 1);
      }
    },

    saveTrustedIssuer(trustedIssuer: TrustedIssuer & { isEdit: boolean }) {
      // update existing trusted issuer
      if (this.isDirty) {
        if (trustedIssuer.id) {
          this.updateTrustedIssuer(trustedIssuer);
        } else {
          this.createNewTrustedIssuer(trustedIssuer);
        }

        this.$emit("changed");
      } else {
        this.editingTrustedIssuer = undefined;
        this.isEdit = false;
        trustedIssuer.isEdit = false;
      }
    },
    createNewTrustedIssuer(trustedIssuer: TrustedIssuer & { isEdit: boolean }) {
      this.isBusy = true;
      let data = Object.assign({}, trustedIssuer);
      delete data.isEdit;
      adminService
        .addTrustedIssuer(this.schema.id, data)
        .then((result) => {
          console.log(result);
          this.isBusy = false;

          if (result.status === 200) {
            this.isEdit = false;
            trustedIssuer.isEdit = false;
            this.editingTrustedIssuer = undefined;
            EventBus.$emit(
              "success",
              this.$t("component.trustedIssuers.eventSuccessCreate")
            );
            this.$emit("changed");
          }
        })
        .catch((error) => {
          this.isBusy = false;
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
    updateTrustedIssuer(trustedIssuer: TrustedIssuer & { isEdit: boolean }) {
      this.isBusy = true;
      let data = Object.assign({}, trustedIssuer);
      delete data.isEdit;

      adminService
        .updateTrustedIssuer(this.schema.id, trustedIssuer.id, data)
        .then((result) => {
          console.log(result);
          this.isBusy = false;
          trustedIssuer.isEdit = false;
          this.editingTrustedIssuer = undefined;

          if (result.status === 200) {
            EventBus.$emit(
              "success",
              this.$t("component.trustedIssuers.eventSuccessUpdate")
            );
            this.$emit("changed");
          }
        })
        .catch((error) => {
          this.isBusy = false;
          trustedIssuer.isEdit = true;
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
  },
};
</script>
