<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container justify-center>
    <v-card class="mx-auto" max-width="400" flat>
      <v-card-title> </v-card-title>
      <v-select
        label="Contact Person"
        v-model="person.type"
        :items="contactPersonTypes"
        outlined
        dense
      ></v-select>

      <v-row>
        <v-col cols="4">
          <v-select
            label="Salutation"
            placeholder=""
            v-model="person.salutation"
            :items="salutationTypes"
            outlined
            dense
          ></v-select>
        </v-col>
        <v-col cols="4">
          <v-select
            label="Academic Title"
            placeholder=""
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
            label="First Name"
            placeholder=""
            v-model="person.firstName"
            outlined
            dense
          ></v-text-field>
        </v-col>
        <v-col cols="6">
          <v-text-field
            label="Last Name"
            placeholder=""
            v-model="person.lastName"
            outlined
            dense
          ></v-text-field>
        </v-col>
      </v-row>

      <v-text-field
        label="E-Mail"
        placeholder=""
        v-model="person.email"
        outlined
        dense
      ></v-text-field>

      <v-text-field
        label="Phone"
        placeholder=""
        v-model="person.phone"
        outlined
        dense
      ></v-text-field>

      <v-text-field
        label="Mobile Phone"
        placeholder=""
        v-model="person.mobilePhone"
        outlined
        dense
      ></v-text-field>

      <v-text-field
        label="Country"
        placeholder=""
        v-model="person.country"
        outlined
        dense
      ></v-text-field>
      <v-card-actions>
        <v-layout justify-space-between>
          <v-bpa-button color="secondary" :to="{ name: 'AddDocument' }"
            >Cancel</v-bpa-button
          >
          <v-bpa-button
            color="primary"
            :to="{
              name: 'AddDocument',
              params: { person: person },
            }"
            >Save</v-bpa-button
          >
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
import VBpaButton from "@/components/BpaButton";
export default {
  name: "ContactPerson",
  components: { VBpaButton },
  props: {
    person: {
      type: Object,
    },
  },
  created() {
    EventBus.$emit("title", "Contact Person");
    if (this.person) {
      this.newContact = true;
    }
  },
  data: () => {
    return {
      newContact: false,
      contactPersonTypes: [
        "CEO",
        "Director Logistics",
        "Director Quality",
        "Compliance Responsible",
        "Product Safety Expert",
        "Director Finance",
        "HSE-Responsible",
      ],
      salutationTypes: ["Mr.", "Mrs."],
      academicTitleTypes: ["Dr.", "Prof. Dr."],
    };
  },
  methods: {},
};
</script>
