<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title class="bg-light">
        {{ $t("component.addSchema.title") }}
      </v-card-title>
      <credential-type-tabs>
        <template v-slot:indy>
          <import-credential-indy
            v-on:cancelled="cancel"
            v-on:success="success"
          />
        </template>
        <template v-slot:json-ld>
          <import-credential-json-ld
            v-on:cancelled="cancel"
            v-on:success="success"
          />
        </template>
      </credential-type-tabs>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import CredentialTypeTabs from "@/components/schema-add/CredentialTypeTabs.vue";
import ImportCredentialIndy from "@/components/schema-add/ImportCredentialIndy.vue";
import ImportCredentialJsonLd from "@/components/schema-add/ImportCredentialJsonLd.vue";

export default {
  name: "SchemaAdd",
  components: {
    ImportCredentialJsonLd,
    ImportCredentialIndy,
    CredentialTypeTabs,
  },
  methods: {
    cancel() {
      this.$emit("cancelled");
    },
    success() {
      this.$emit("success");
    },
  },
};
</script>
