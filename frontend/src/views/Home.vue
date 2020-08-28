<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container fill-height fluid text-center>
    <v-row align="center" justify="center">
      <v-col>
        <h1 v-bind:style="{ fontSize: `700%` }" class="grey--text text--lighten-2">Welcome</h1>
        <p  v-bind:style="{ fontSize: `180%` }" class="grey--text text--darken-2 font-weight-medium">What do you want to do?</p>
        <br />
        <v-btn color="primary" :to="{ name: 'Wallet' }">Setup your Profile</v-btn>
        <br />
        <v-btn  :to="{ name: 'Partners' }" text>Connect with Business Partners</v-btn>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
export default {
  name: "Home",
  created() {
    EventBus.$emit("title", "");
  },
};
</script>

