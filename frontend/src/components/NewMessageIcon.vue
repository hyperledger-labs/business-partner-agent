<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <div class="newIconContainer">
    <span>
      <v-tooltip right>
        <template v-slot:activator="{ on, attrs }">
          <v-icon
            small
            :color="color"
            v-show="show"
            :class="{ newIconCredential: isCredential }"
            v-bind="attrs"
            v-on="on"
            >$vuetify.icons.newMessage</v-icon
          >
        </template>
        <span>{{ this.text }}</span>
      </v-tooltip>
    </span>
  </div>
</template>

<script lang="ts">
export default {
  name: "NewMessageIcon",
  props: {
    type: {
      type: String,
      default: "",
    },
    id: {
      type: String,
      default: "",
    },
  },
  computed: {
    color: function () {
      // are we going to need different colors for each type ?
      // partner will also have a partner state... so could be confusing ?
      return "green";
    },
    isCredential: function () {
      return this.type === "credential";
    },
    show: function () {
      // if this id is in the specified collection, then show it
      let result;
      switch (this.type) {
        case "activity":
          result = Object.prototype.hasOwnProperty.call(
            this.$store.getters.activityNotifications,
            this.id
          );
          break;
        case "credential":
          result = Object.prototype.hasOwnProperty.call(
            this.$store.getters.credentialNotifications,
            this.id
          );
          break;
        case "partner":
          result = Object.prototype.hasOwnProperty.call(
            this.$store.getters.partnerNotifications,
            this.id
          );
          break;
        case "presentation":
          result = Object.prototype.hasOwnProperty.call(
            this.$store.getters.presentationNotifications,
            this.id
          );
          break;
        case "task":
          result = Object.prototype.hasOwnProperty.call(
            this.$store.getters.taskNotifications,
            this.id
          );
          break;
        default:
          result = false;
      }
      return result;
    },
    text: function () {
      // if this id is in the specified collection, then show it
      let result = "";
      switch (this.type) {
        case "activity":
          result = this.$t("component.newMessageIcon.activity");
          break;
        case "credential":
          result = this.$t("component.newMessageIcon.credential");
          break;
        case "partner":
          result = this.$t("component.newMessageIcon.partner");
          break;
        case "presentation":
          result = this.$t("component.newMessageIcon.presentation");
          break;
        case "task":
          result = this.$t("component.newMessageIcon.task");
          break;
        default:
          result = this.$t("component.newMessageIcon.default");
      }
      return result;
    },
  },
};
</script>

<style scoped>
.newIconContainer {
  position: relative;
  float: left;
}

.newIconCredential {
  right: 2px;
}

.setTop {
  top: 0px;
}
</style>
