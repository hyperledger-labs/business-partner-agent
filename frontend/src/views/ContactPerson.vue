<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container justify-center>
    <v-card class="mx-auto" max-width="400" flat>
      <v-card-title> </v-card-title>
      <v-select
        :label="$t('view.contactPerson.labelSelect')"
        v-model="person.type"
        :items="contactPersonTypes"
        outlined
        dense
      ></v-select>

      <v-row>
        <v-col cols="4">
          <v-select
            :label="$t('view.contactPerson.labelSalutation')"
            v-model="person.salutation"
            :items="salutationTypes"
            outlined
            dense
          ></v-select>
        </v-col>
        <v-col cols="4">
          <v-select
            :label="$t('view.contactPerson.labelAcademicTitle')"
            v-model="person.academicTitle"
            :items="academicTitleTypes"
            outlined
            dense
          ></v-select>
        </v-col>
      </v-row>

      <v-row>
        <v-col cols="6">
          <v-text-field
            :label="$t('view.contactPerson.labelFirstName')"
            v-model="person.firstName"
            outlined
            dense
          ></v-text-field>
        </v-col>
        <v-col cols="6">
          <v-text-field
            :label="$t('view.contactPerson.labelLastName')"
            v-model="person.lastName"
            outlined
            dense
          ></v-text-field>
        </v-col>
      </v-row>

      <v-text-field
        :label="$t('view.contactPerson.labelMail')"
        v-model="person.email"
        outlined
        dense
      ></v-text-field>

      <v-text-field
        :label="$t('view.contactPerson.labelPhone')"
        v-model="person.phone"
        outlined
        dense
      ></v-text-field>

      <v-text-field
        :label="$t('view.contactPerson.labelMobile')"
        v-model="person.mobilePhone"
        outlined
        dense
      ></v-text-field>

      <v-text-field
        :label="$t('view.contactPerson.labelCountry')"
        v-model="person.country"
        outlined
        dense
      ></v-text-field>
      <v-card-actions>
        <v-layout justify-space-between>
          <v-bpa-button color="secondary" :to="{ name: 'AddDocument' }">{{
            $t("button.cancel")
          }}</v-bpa-button>
          <v-bpa-button
            color="primary"
            :to="{
              name: 'AddDocument',
              params: { person: person },
            }"
            >{{ $t("button.save") }}</v-bpa-button
          >
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import VBpaButton from "@/components/BpaButton";
export default {
  name: "ContactPerson",
  components: { VBpaButton },
  props: {
    value: {},
  },
  computed: {
    person: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit("input", value);
      },
    },
    contactPersonTypes() {
      return [
        this.$t("view.contactPerson.contactPersonTypes.ceo"),
        this.$t("view.contactPerson.contactPersonTypes.directorLogistics"),
        this.$t("view.contactPerson.contactPersonTypes.directorQuality"),
        this.$t("view.contactPerson.contactPersonTypes.complianceResponsible"),
        this.$t("view.contactPerson.contactPersonTypes.productSafetyExpert"),
        this.$t("view.contactPerson.contactPersonTypes.directorFinance"),
        this.$t("view.contactPerson.contactPersonTypes.hseResponsible"),
      ];
    },
    salutationTypes() {
      return [
        this.$t("view.contactPerson.salutationTypes.mr"),
        this.$t("view.contactPerson.salutationTypes.mrs"),
      ];
    },
    academicTitleTypes() {
      return [
        this.$t("view.contactPerson.academicTitleTypes.dr"),
        this.$t("view.contactPerson.academicTitleTypes.profDr"),
      ];
    },
  },
  created() {
    EventBus.$emit("title", this.$t("view.contactPerson.title"));
    if (this.person) {
      this.newContact = true;
    }
  },
  data: () => {
    return {
      newContact: false,
    };
  },
};
</script>
