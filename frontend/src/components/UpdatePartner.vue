<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title class="bg-light mb-4"> Update Partner </v-card-title>
      <v-card-text>
        <v-row>
          <v-col cols="4">
            <p class="grey--text text--darken-2 font-weight-medium">Name</p>
          </v-col>
          <v-col cols="8">
            <v-text-field
              label="Name"
              placeholder=""
              v-model="alias"
              outlined
              dense
            >
            </v-text-field>
          </v-col>
        </v-row>
        <v-row>
          <v-col cols="4">
            <p class="grey--text text--darken-2 font-weight-medium">Tags</p>
          </v-col>
          <v-col cols="8">
            <v-autocomplete
              multiple
              v-model="selectedTags"
              label="Tags"
              :items="tags"
              chips
              deletable-chips
            >
            </v-autocomplete>
          </v-col>
          <v-list-item>
            <v-list-item-title
              class="grey--text text--darken-2 font-weight-medium"
              >{{ $t("view.addPartner.trustPing") }}</v-list-item-title
            >

            <v-list-item-action>
              <v-switch v-model="trustPing"></v-switch>
            </v-list-item-action>
          </v-list-item>
        </v-row>
      </v-card-text>
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-bpa-button color="secondary" @click="cancel()"
            >Cancel</v-bpa-button
          >
          <v-btn :loading="this.isBusy" color="primary" text @click="submit()"
            >Submit</v-btn
          >
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import VBpaButton from "@/components/BpaButton";
import partnerService from "@/services/partnerService";

export default {
  name: "UpdatePartner",
  components: { VBpaButton },
  props: {
    partner: {
      type: Object,
      default: () => {},
    },
  },
  data: () => {
    return {
      isBusy: false,
      updatedTags: [],
      rules: {
        required: (value) => !!value || "Can't be empty",
      },
    };
  },
  mounted() {
    this.updatedTags = this.selectedTags;
    this.updatedAlias = this.alias;
    this.updatedTrustPing = this.trustPing;
  },
  computed: {
    alias: {
      get() {
        return this.partner.alias;
      },
      set(data) {
        this.updatedAlias = data;
      },
    },
    selectedTags: {
      get() {
        return this.partner && this.partner.tag
          ? this.partner.tag.map((tag) => tag.name)
          : [];
      },

      set(data) {
        this.updatedTags = data;
      },
    },
    trustPing: {
      get() {
        return this.partner.trustPing;
      },
      set(data) {
        this.updatedTrustPing = data;
      },
    },
    tags() {
      return this.$store.state.tags
        ? this.$store.state.tags.map((tag) => tag.name)
        : [];
    },
  },
  watch: {},
  methods: {
    async submit() {
      this.isBusy = true;

      const data = {
        alias: this.updatedAlias,
        tag: this.$store.state.tags.filter((tag) => {
          return this.updatedTags.includes(tag.name);
        }),
        trustPing: this.updatedTrustPing,
      };

      partnerService
        .updatePartner({ id: this.partner.id, data })
        .then((result) => {
          this.isBusy = false;
          if (result.status === 200) {
            EventBus.$emit("success", "Parttner updated successfully");
            this.$emit("success");
          }
        })
        .catch((e) => {
          this.isBusy = false;
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    cancel() {
      this.$emit("cancelled");
    },
  },
};
</script>
