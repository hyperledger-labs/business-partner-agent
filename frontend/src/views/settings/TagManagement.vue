<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container justify-center>
    <v-card class="mx-auto">
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        <span>{{ $t("view.tagManagement.title") }}</span>
      </v-card-title>
      <v-card-text>
        <div class="my-8"></div>
        <v-row class="mx-2">
          <v-col cols="4">
            <p class="grey--text text--darken-2 font-weight-medium">
              {{ $t("view.tagManagement.availableTags") }}
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
              {{ $t("view.tagManagement.addNewTag") }}
            </p>
          </v-col>
          <v-col cols="6">
            <v-text-field
              :label="$t('view.tagManagement.labelName')"
              v-model="newTag"
              outlined
              dense
            >
            </v-text-field>
          </v-col>
          <v-col cols="2">
            <v-btn color="primary" :disabled="fieldEmpty" @click="addNewTag">
              {{ $t("view.tagManagement.addTag") }}
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>
    <v-dialog v-model="hardDeleteDialog" max-width="450">
      <v-card>
        <v-card-title class="headline">{{
          $t("view.tagManagement.deleteTag")
        }}</v-card-title>
        <v-card-text>
          <p>{{ deleteErrorMsg }}</p>

          <p>
            {{ $t("view.tagManagement.confirmMessageDeleteTag") }}
          </p>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-bpa-button color="secondary" @click="hardDeleteDialog = false">{{
            $t("button.no")
          }}</v-bpa-button>
          <v-bpa-button color="error" @click="deleteTag(selectedTag, true)">{{
            $t("button.yes")
          }}</v-bpa-button>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import adminService from "@/services/adminService";
import VBpaButton from "@/components/BpaButton";

export default {
  name: "TagManagement",
  components: { VBpaButton },
  created() {
    EventBus.$emit("title", this.$t("view.tagManagement.title"));
  },
  data: () => {
    return {
      newTag: "",
      hardDeleteDialog: false,
      deleteErrorMsg: "",
      selectedTag: "",
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
        .then((response) => {
          if (response.status === 201 || response.status === 200) {
            EventBus.$emit(
              "success",
              this.$t("view.tagManagement.eventSuccessTagAdded")
            );
            this.newTag = "";
            this.$store.dispatch("loadTags");
          } else {
            EventBus.$emit("error", response.statusText);
          }
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
    deleteTag(tag, hardDelete = false) {
      adminService
        .deleteTag(tag, hardDelete)
        .then((response) => {
          this.hardDeleteDialog = false;
          if (response.status === 201 || response.status === 200) {
            EventBus.$emit(
              "success",
              this.$t("view.tagManagement.eventSuccessTagRemoved")
            );
            this.$store.dispatch("loadTags");
            this.deleteErrorMsg = "";
            this.selectedTag = undefined;
          } else {
            EventBus.$emit("error", response.statusText);
          }
        })
        .catch((error) => {
          console.error(error.response);
          if (error.response.status === 400) {
            this.hardDeleteDialog = true;
            this.deleteErrorMsg = error.response.data.message;
            this.selectedTag = tag;
          } else {
            EventBus.$emit("error", this.$axiosErrorMessage(error));
          }
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
