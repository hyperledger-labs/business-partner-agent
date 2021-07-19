<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title class="bg-light">
        <span>{{ proofTemplate.name }}</span>
        <v-layout align-end justify-end>
          <v-btn depressed color="red" icon @click="deleteProofTemplate">
            <v-icon dark>$vuetify.icons.delete</v-icon>
          </v-btn>
        </v-layout>
      </v-card-title>
      <v-container>
        <v-list-item class="mt-4">
          <v-text-field
            id="proofTemplateName"
            v-model="proofTemplate.name"
            readonly
            dense
            label="Name"
            :append-icon="'$vuetify.icons.copy'"
          ></v-text-field>
        </v-list-item>
        <v-list-item class="mt-4">
          <v-text-field
            id="proofTemplateCreatedAt"
            v-model="proofTemplate.createdAt"
            readonly
            dense
            label="Created At"
            :append-icon="'$vuetify.icons.copy'"
          ></v-text-field>
        </v-list-item>
      </v-container>
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-bpa-button color="primary" @click="closed">Close</v-bpa-button>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import proofTemplateService from "@/services/proofTemplateService";
import VBpaButton from "@/components/BpaButton";
export default {
  name: "ViewProofTemplate",
  props: {
    dialog: {
      type: Boolean,
      default: () => false,
    },
    proofTemplate: Object,
  },
  components: {
    VBpaButton,
  },
  watch: {},
  created() {},
  data: () => {},
  computed: {},
  methods: {
    deleteProofTemplate() {
      proofTemplateService
        .deleteProofTemplate(this.proofTemplate.id)
        .then((result) => {
          console.log(result);
          if (result.status === 200) {
            EventBus.$emit("success", "Proof Template deleted");
            this.$emit("changed");
            this.$emit("deleted");
          }
        })
        .catch((e) => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    onChanged() {
      this.$emit("changed");
    },
    closed() {
      this.$emit("closed");
    },
  },
};
</script>
