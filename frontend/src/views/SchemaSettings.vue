<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container justify-center>
    <v-card class="mx-auto">
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        <span>Schemas</span>
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
              <v-bpa-button v-bind="attrs" v-on="on" color="secondary"
              >Create Schema</v-bpa-button
              >
            </template>
            <CreateSchema
                @success="onSchemaCreated"
                @cancelled="createSchemaDialog = false"
            />
          </v-dialog>
          <v-dialog v-model="addSchemaDialog" persistent max-width="600px">
            <template v-slot:activator="{ on, attrs }">
              <v-bpa-button v-bind="attrs" v-on="on" color="primary"
                >Import Schema</v-bpa-button
              >
            </template>
            <AddSchema
              @success="onSchemaAdded"
              @cancelled="addSchemaDialog = false"
            />
          </v-dialog>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
import SchemaList from "@/components/SchemaList";
import AddSchema from "@/components/AddSchema";
import CreateSchema from "@/components/CreateSchema";

import store from "@/store";
import VBpaButton from "@/components/BpaButton";

export default {
  name: "SchemaSettings",
  components: {
    VBpaButton,
    AddSchema,
    CreateSchema,
    SchemaList,
  },
  created() {
    EventBus.$emit("title", "Schema Settings");
  },
  data: () => {
    return {
      addSchemaDialog: false,
      createSchemaDialog: false,
    };
  },
  computed: {},
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
