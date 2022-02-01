<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container justify-center>
    <v-card class="my-4 mx-auto">
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        <span>{{ $t("view.schemaSettings.title") }}</span>
      </v-card-title>
      <v-card-text>
        <SchemaList
          :manage-credential-definitions="true"
          :manage-trusted-issuers="true"
        />
      </v-card-text>
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-dialog v-model="createSchemaDialog" persistent max-width="600px">
            <template v-slot:activator="{ on, attrs }">
              <v-bpa-button v-bind="attrs" v-on="on" color="secondary">{{
                $t("view.schemaSettings.buttonCreateSchema")
              }}</v-bpa-button>
            </template>
            <CreateSchema
              @success="onSchemaCreated"
              @cancelled="createSchemaDialog = false"
            />
          </v-dialog>
          <v-dialog v-model="addSchemaDialog" persistent max-width="800px">
            <template v-slot:activator="{ on, attrs }">
              <v-bpa-button v-bind="attrs" v-on="on" color="primary">{{
                $t("view.schemaSettings.buttonImportSchema")
              }}</v-bpa-button>
            </template>
            <SchemaAdd
              @success="onSchemaAdded"
              @cancelled="addSchemaDialog = false"
            />
          </v-dialog>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import SchemaList from "@/components/SchemaList.vue";
import SchemaAdd from "@/components/schema-add/SchemaAdd.vue";
import CreateSchema from "@/components/CreateSchema.vue";
import store from "@/store";
import VBpaButton from "@/components/BpaButton";

export default {
  name: "SchemaSettings",
  components: {
    VBpaButton,
    SchemaAdd,
    CreateSchema,
    SchemaList,
  },
  created() {
    EventBus.$emit("title", this.$t("view.schemaSettings.title"));
  },
  data: () => {
    return {
      addSchemaDialog: false,
      createSchemaDialog: false,
    };
  },
  methods: {
    onSchemaAdded() {
      store.dispatch("loadSchemas");
      this.addSchemaDialog = false;
    },
    onSchemaCreated() {
      store.dispatch("loadSchemas");
      this.createSchemaDialog = false;
    },
  },
};
</script>

<style scoped>
.bg-light {
  background-color: #fafafa;
}

.bg-light-2 {
  background-color: #ececec;
}
</style>
