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
        <span>Tag Management</span>
      </v-card-title>
      <v-card-text>
        <div class="my-8"></div>
        <v-row class="mx-2">
          <v-col cols="4">
            <p class="grey--text text--darken-2 font-weight-medium">
              Available tags
            </p>
          </v-col>
          <v-col cols="8">
            <v-chip
              class="ml-2"
              v-for="tag in tags"
              :key="tag.id"
              :close="!tag.isReadOnly"
              @click:close="deleteTag(tag.id)"
            >
              {{ tag.name }}
            </v-chip>
          </v-col>
        </v-row>
        <v-row class="mx-2">
          <v-col cols="4">
            <p class="grey--text text--darken-2 font-weight-medium">
              Add a new tag
            </p>
          </v-col>
          <v-col cols="6">
            <v-text-field
              label="Name"
              placeholder=""
              v-model="newTag"
              outlined
              dense
            >
            </v-text-field>
          </v-col>
          <v-col cols="2">
            <v-btn :disabled="fieldEmpty" @click="addNewTag"> Add tag </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
import adminService from "@/services/adminService";

export default {
  name: "TagManagement",
  components: {},
  created() {
    EventBus.$emit("title", "Tag Management");
  },
  data: () => {
    return {
      newTag: "",
    };
  },
  computed: {
    tags() {
      return this.$store.state.tags;
    },
    fieldEmpty() {
      return this.newTag.length === 0;
    },
  },
  methods: {
    addNewTag() {
      adminService
        .addTag({
          name: this.newTag,
        })
        .then((res) => {
          if (res.status === 201 || res.status === 200) {
            EventBus.$emit("success", "Tag successfully added");
            this.$store.dispatch("loadTags");
          } else {
            EventBus.$emit("error", res.status.text);
          }
        })
        .catch((e) => {
          console.error(e);
          this.isBusy = false;
          EventBus.$emit("error", e);
        });
    },
    deleteTag(tag) {
      adminService
        .deleteTag(tag)
        .then((res) => {
          if (res.status === 201 || res.status === 200) {
            EventBus.$emit("success", "Tag successfully removed");
            this.$store.dispatch("loadTags");
          } else {
            EventBus.$emit("error", res.status.text);
          }
        })
        .catch((e) => {
          console.error(e);
          this.isBusy = false;
          EventBus.$emit("error", e);
        });
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
