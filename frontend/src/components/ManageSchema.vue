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
        <span>{{ data.label }}</span>
        <v-layout align-end justify-end>
          <v-btn
              depressed
              color="red"
              icon
              :disabled="data.isReadOnly"
              @click="deleteSchema"
          >
            <v-icon dark>$vuetify.icons.delete</v-icon>
          </v-btn>
        </v-layout>
      </v-card-title>
      <v-container>
        <v-list-item class="mt-4">
          <v-list-item-title
              class="grey--text text--darken-2 font-weight-medium"
          >
            Schema ID:
          </v-list-item-title>
          <v-list-item-subtitle>
            {{ data.schemaId }}
          </v-list-item-subtitle>
        </v-list-item>

        <h4 class="my-4 grey--text text--darken-3">Schema Attributes</h4>

        <v-list-item
            v-for="attribute in data.schemaAttributeNames"
            :key="attribute.id"
        >
          <p class="grey--text text--darken-2 font-weight-medium">
            {{ attribute }}
          </p>
        </v-list-item>
        <v-card>
          <v-card-title class="bg-light">Credential Definitions</v-card-title>
          <v-data-table
              :hide-default-footer="credentialDefinitions.length < 10"
              :headers="headers"
              :items="credentialDefinitions"
              :loading="isLoading"
          >
            <template #[`item.createdAt`]="{ item }">
              {{ item.createdAt | formatDateLong }}
            </template>
          </v-data-table>
          <v-card-actions>
            <v-btn
                v-if="!addingNewCredDef"
                color="primary"
                small
                dark
                absolute
                bottom
                left
                fab
                @click="addNewCredDef"
            >
              <v-icon>$vuetify.icons.add</v-icon>
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-container>
      <v-container v-if="addingNewCredDef">
        <v-card>
          <v-card-title>Add New Credential Definition</v-card-title>
          <v-container>
            <v-form v-model="isCredDefFormValid">
              <v-row>
                <v-col class="pb-0">
                  <v-text-field
                      label="Tag"
                      class="mt-6"
                      placeholder="Tag"
                      v-model="tag"
                      outlined
                      dense
                      required
                      :rules="tagRules"
                  >
                  </v-text-field>
                </v-col>
                <v-col cols="4" class="pb-0">
                  <v-checkbox
                      class="mt-6"
                      label="Revocable"
                      v-model="supportRevocation"
                      outlined
                      dense
                  >
                  </v-checkbox>
                </v-col>
              </v-row>
              <v-card-actions>
                <v-layout align-end justify-end>
                  <v-btn color="primary" text @click="submitCredDef" :loading="isSubmittingCredDef" :disabled="!isCredDefFormValid">Save</v-btn>
                  <v-btn color="secondary" text @click="cancelCredDef">Cancel</v-btn>
                </v-layout>
              </v-card-actions>
            </v-form>
          </v-container>
        </v-card>
      </v-container>
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-btn color="primary" text :disabled="addingNewCredDef" @click="closed"
          >Close</v-btn>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>

  import {EventBus} from "@/main";
  import {issuerService} from "@/services";

  export default {
    name: "ManageSchema",
    props: {
      schema: Object,
    },
    components: {
    },
    watch: {
      schema(val) {
        // schema has been updated...
        if (val) {
          this.load();
        }
      }
    },
    created() {
      console.log("Schema: ", this.schema);
      this.load();
    },
    data: () => {
      return {
        data: [],
        credentialDefinitions: [],
        headers: [
          {
            text: "Credential Definition Id",
            value: "credentialDefinitionId",
          },
          {
            text: "Tag",
            value: "tag",
          },
          {
            text: "Revocable",
            value: "isSupportRevocation",
          },
          {
            text: "Created Date",
            value: "createdAt",
          },
        ],
        isCredDefFormValid: false,
        isSubmittingCredDef: false,
        addingNewCredDef: false,
        tag: "",
        supportRevocation: false,
      };
    },
    computed: {
      tagRules() {
        return [
          (value) => {
            if (this.addingNewCredDef) {
              // value is required
              return !!value || "Can't be empty";
            }
            return true;
          },
          (value) => {
            if (this.addingNewCredDef && value) {
              const o = this.credentialDefinitions.find(c => c.tag.toUpperCase() === value.toUpperCase());
              return (o === undefined) || "Tag must be unique";
            }
            return true;
          }
        ]
      },
    },
    methods: {
      load() {
        this.data = this.schema;
        this.credentialDefinitions = this.schema.credentialDefinitions;
      },
      deleteSchema() {
        this.$axios
          .delete(`${this.$apiBaseUrl}/admin/schema/${this.id}`)
          .then((result) => {
            console.log(result);
            if (result.status === 200) {
              EventBus.$emit("success", "Schema deleted");
              this.$emit("schemaDeleted");
            }
          })
          .catch((e) => {
            console.error(e);
            EventBus.$emit("error", e);
          });
      },
      addNewCredDef() {
        if (!this.addingNewCredDef) this.addingNewCredDef = true;
      },
      cancelCredDef() {
        if (this.addingNewCredDef) {
          this.addingNewCredDef = false;
          this.tag ="";
          this.supportRevocation = false;
        }
      },
      async submitCredDef() {
        this.isSubmittingCredDef = true;
        try {
          const credDefForm =  {
            schemaId: this.data.schemaId,
            tag: this.tag,
            supportRevocation: this.supportRevocation,
          }
          const resp = await issuerService.createCredDef(credDefForm);
          if (resp.status === 200) {
            this.isSubmittingCredDef = false;
            EventBus.$emit("success", "Credential Definition added");
            this.$emit("credDefAdded");
            // reload the data
            const r = await issuerService.readSchema(this.id);
            if (r.status === 200) {
              this.data = r.data;
              this.credentialDefinitions = r.data.credentialDefinitions;
            }
          }
          this.cancelCredDef();
          this.isSubmittingCredDef = false;
        } catch(error) {
          this.isSubmittingCredDef = false;
          EventBus.$emit("error", error);
        }
      },
      closed() {
        this.$emit("closed");
      },
    },
  };
</script>
