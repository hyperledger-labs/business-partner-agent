<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-row>
      <v-col cols="4" class="pb-0">
        <p class="grey--text text--darken-2 font-weight-medium">
          {{ $t("component.organizationProfile.companyInfo.sectionTitle") }}
        </p>
      </v-col>
      <v-col cols="8" class="pb-0">
        <v-text-field
          :label="$t('component.organizationProfile.companyInfo.type')"
          v-model="documentData.type"
          outlined
          disabled
          dense
        ></v-text-field>
        <v-text-field
          :label="$t('component.organizationProfile.companyInfo.companyName')"
          :disabled="isReadOnly"
          :rules="[(v) => !!v || $t('app.rules.required')]"
          required
          outlined
          dense
          @change="onLegalNameChange($event)"
          :value="documentData.legalName"
        ></v-text-field>
        <v-text-field
          :label="
            $t('component.organizationProfile.companyInfo.companyAltName')
          "
          v-model="documentData.altName"
          :disabled="isReadOnly"
          required
          outlined
          dense
        ></v-text-field>
        <v-row
          v-for="(identifier, index) in documentData.identifier"
          v-bind:key="identifier.type"
        >
          <v-col cols="4" class="py-0">
            <v-select
              :label="
                $t('component.organizationProfile.companyInfo.identifier')
              "
              v-model="identifier.type"
              :items="identifierTypes"
              :disabled="isReadOnly"
              outlined
              dense
            ></v-select>
          </v-col>
          <v-col class="py-0">
            <v-text-field
              v-model="identifier.id"
              :disabled="isReadOnly"
              outlined
              dense
            ></v-text-field>
          </v-col>
          <v-col v-if="!isReadOnly" cols="2" class="py-0">
            <v-layout>
              <v-bpa-button
                v-if="
                  !isReadOnly && index === documentData.identifier.length - 1
                "
                color="secondary"
                @click="addIdentifier"
                >Add</v-bpa-button
              >
              <v-btn
                icon
                v-if="
                  !isReadOnly && index !== documentData.identifier.length - 1
                "
                @click="deleteIdentifier(index)"
              >
                <v-icon color="error">$vuetify.icons.delete</v-icon>
              </v-btn>
            </v-layout>
          </v-col>
        </v-row>
      </v-col>
    </v-row>
    <v-divider class="my-4"></v-divider>
    <v-row>
      <v-col cols="4" class="pb-0">
        <p class="grey--text text--darken-2 font-weight-medium">
          {{ $t("component.organizationProfile.addressInfo.sectionTitle") }}
        </p>
      </v-col>
      <v-col cols="8" class="pb-0">
        <v-text-field
          :label="$t('component.organizationProfile.addressInfo.street')"
          v-model="documentData.registeredSite.address.streetAddress"
          :disabled="isReadOnly"
          outlined
          dense
        ></v-text-field>
        <v-row>
          <v-col cols="4" class="py-0">
            <v-text-field
              :label="
                $t('component.organizationProfile.addressInfo.postalCode')
              "
              v-model="documentData.registeredSite.address.zipCode"
              :disabled="isReadOnly"
              outlined
              dense
            ></v-text-field>
          </v-col>
          <v-col cols="8" class="py-0">
            <v-text-field
              :label="$t('component.organizationProfile.addressInfo.city')"
              v-model="documentData.registeredSite.address.city"
              :disabled="isReadOnly"
              outlined
              dense
            ></v-text-field>
          </v-col>
        </v-row>
        <v-row>
          <v-col cols="6" class="py-0">
            <v-text-field
              :label="$t('component.organizationProfile.addressInfo.country')"
              v-model="documentData.registeredSite.address.country"
              :disabled="isReadOnly"
              outlined
              dense
            ></v-text-field>
          </v-col>
          <v-col cols="6" class="py-0">
            <v-text-field
              :label="$t('component.organizationProfile.addressInfo.region')"
              v-model="documentData.registeredSite.address.region"
              :disabled="isReadOnly"
              outlined
              dense
            ></v-text-field>
          </v-col>
        </v-row>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import { profileModel } from "@/models/model";
import VBpaButton from "@/components/BpaButton";
export default {
  props: {
    isReadOnly: Boolean,
    value: {
      type: Object,
      default: () => profileModel,
    },
  },
  created() {
    this.intDoc.documentData = this.documentData;
    this.intDoc.label = this.documentData.legalName;
  },
  data: () => {
    return {
      identifierTypes: ["LEI", "GLN", "D-U-N-S", "VAT", "USCC"],
      intDoc: Object,
    };
  },
  computed: {
    documentData: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit("input", value);
      },
    },
  },
  methods: {
    addIdentifier() {
      this.documentData.identifier.push({
        id: "",
        type: "",
      });
    },
    deleteIdentifier(index) {
      this.documentData.identifier.splice(index, 1);
    },
    onLegalNameChange(event) {
      this.documentData.legalName = event;
      this.intDoc.label = event;
    },
  },
  components: { VBpaButton },
};
</script>
