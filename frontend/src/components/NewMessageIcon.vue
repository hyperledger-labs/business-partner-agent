<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <div class="newIconContainer">
    <v-icon
      color="green"
      class="newIcon"
      small
      v-bind:class="{
        newIconPartner: isPartner,
        newIconCredential: !isPartner,
        setTop: isEmpty,
      }"
    >
      $vuetify.icons.newMessage
    </v-icon>
  </div>
</template>

<script>
export default {
  name: "NewMessageIcon",
  props: {
    isPartner: {
      type: Boolean,
      default: false,
    },
    text: {
      type: String,
      default: "",
    },
  },
  computed: {
    isEmpty() {
      return this.text.length === 0;
    },
  },
};
</script>

<style scoped>
.newIconContainer {
  position: relative;
  float: left;
}

.newIcon {
  position: absolute;
  bottom: 0;
  top: 10px;
}

.newIconPartner {
  right: 0px;
}

.newIconCredential {
  right: 2px;
}

.setTop {
  top: 0px;
}
</style>
