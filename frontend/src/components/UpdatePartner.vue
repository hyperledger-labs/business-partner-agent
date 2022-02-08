<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title class="bg-light mb-4">{{
        $t("component.updatePartner.title")
      }}</v-card-title>
      <v-card-text>
        <v-row>
          <v-col cols="4">
            <p class="grey--text text--darken-2 font-weight-medium">
              {{ $t("component.updatePartner.name") }}
            </p>
          </v-col>
          <v-col cols="8">
            <v-text-field
              :label="$t('component.updatePartner.labelName')"
              v-model="alias"
              outlined
              dense
              :rules="[rules.required]"
            >
            </v-text-field>
          </v-col>
        </v-row>
        <v-row>
          <v-col cols="4">
            <p class="grey--text text--darken-2 font-weight-medium">
              {{ $t("component.updatePartner.tags") }}
            </p>
          </v-col>
          <v-col cols="8">
            <v-autocomplete
              multiple
              v-model="selectedTags"
              :label="$t('component.updatePartner.labelTags')"
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
          <v-bpa-button color="secondary" @click="cancel()">{{
            $t("button.cancel")
          }}</v-bpa-button>
          <v-btn
            :loading="this.isBusy"
            color="primary"
            text
            @click="submit()"
            >{{ $t("button.submit") }}</v-btn
          >
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import VBpaButton from "@/components/BpaButton";
import partnerService from "@/services/partnerService";
import { Tag, UpdatePartnerRequest } from "@/services/partner-types";

export default {
  name: "UpdatePartner",
  components: { VBpaButton },
  props: {
    partner: {
      type: Object,
    },
  },
  data: () => {
    return {
      isBusy: false,
      updatedTags: new Array<string>(),
    };
  },
  mounted() {
    this.updatedTags = this.selectedTags;
    this.updatedAlias = this.alias;
    this.updatedTrustPing = this.trustPing;
  },
  computed: {
    rules() {
      return {
        required: (value: string) => !!value || this.$t("app.rules.required"),
      };
    },
    alias: {
      get() {
        return this.partner.alias;
      },
      set(data: string) {
        this.updatedAlias = data;
      },
    },
    selectedTags: {
      get() {
        return this.partner && this.partner.tag
          ? this.partner.tag.map((tag: Tag) => tag.name)
          : [];
      },

      set(data: string[]) {
        this.updatedTags = data;
      },
    },
    trustPing: {
      get() {
        return this.partner.trustPing;
      },
      set(data: boolean) {
        this.updatedTrustPing = data;
      },
    },
    tags() {
      return this.$store.state.tags
        ? this.$store.state.tags.map((tag: Tag) => tag.name)
        : [];
    },
  },
  methods: {
    async submit() {
      this.isBusy = true;

      const data: UpdatePartnerRequest = {
        alias: this.updatedAlias,
        tag: this.$store.state.tags.filter((tag: Tag) => {
          return this.updatedTags.includes(tag.name);
        }),
        trustPing: this.updatedTrustPing,
      };

      partnerService
        .updatePartner(this.partner.id, data)
        .then((result) => {
          this.isBusy = false;
          if (result.status === 200) {
            EventBus.$emit(
              "success",
              this.$t("component.updatePartner.eventSuccess")
            );
            this.$emit("success");
          }
        })
        .catch((error) => {
          this.isBusy = false;
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
    cancel() {
      this.$emit("cancelled");
    },
  },
};
</script>
