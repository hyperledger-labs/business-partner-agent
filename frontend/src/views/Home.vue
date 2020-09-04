<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container text-center>
        <div v-if="isWelcome">
            <!-- Image from undraw.co -->
            <v-img class="mx-auto" src="@/assets/undraw_welcome_3gvl_grey.png" max-width="50%" aspect-ratio="1"></v-img>
            <p  v-bind:style="{ fontSize: `180%` }" class="grey--text text--darken-2 font-weight-medium">Hi, we've already set up an identity for you!</p>
            <p  v-bind:style="{ fontSize: `140%` }" class="grey--text text--darken-2 font-weight-medium">Start by adding a public profile that your business partners will see</p>
            <br />
            <v-btn color="primary" :to="{ name: 'Profile', params: { add: true} }">Setup your Profile</v-btn>
        </div>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
export default {
  name: "Home",
  created() {
    EventBus.$emit("title", "");
  },
  data: () => {
        return {
            isWelcome: true
        };
    },
};
</script>

