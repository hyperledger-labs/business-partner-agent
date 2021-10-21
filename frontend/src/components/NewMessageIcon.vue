<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

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

<script>
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
      let result = false;
      switch (this.type) {
        case "activity":
          result = {}.hasOwnProperty.call(
            this.$store.getters.activityNotifications,
            this.id
          );
          break;
        case "credential":
          result = {}.hasOwnProperty.call(
            this.$store.getters.credentialNotifications,
            this.id
          );
          break;
        case "partner":
          result = {}.hasOwnProperty.call(
            this.$store.getters.partnerNotifications,
            this.id
          );
          break;
        case "presentation":
          result = {}.hasOwnProperty.call(
            this.$store.getters.presentationNotifications,
            this.id
          );
          break;
        case "task":
          result = {}.hasOwnProperty.call(
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
          result = "New activity";
          break;
        case "credential":
          result = "New activity on Credential";
          break;
        case "partner":
          result = "New activity related to this Partner";
          break;
        case "presentation":
          result = "New activity on Presentation";
          break;
        case "task":
          result = "New task requires attention";
          break;
        default:
          result = "New activity on item";
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
