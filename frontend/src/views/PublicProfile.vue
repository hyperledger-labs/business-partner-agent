<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <div v-if="document">
        <OganizationalProfile v-bind:document="document" isReadOnly></OganizationalProfile>
        <DocumentCredentialList v-if="!isBusy" v-bind:credentials="publicDocumentsAndCredentials" isReadOnly showOnlyContent></DocumentCredentialList>
    </div>
      <v-container v-else fill-height fluid text-center>
      <v-row align="center" justify="center">
        <v-col>
          <h1 class="grey--text text--lighten-2">You don't have set up a public profile yet</h1>
          <v-btn color="primary" :to="{ name: 'Wallet' }" text>Setup your Profile</v-btn>
        </v-col>
      </v-row>
    </v-container>
</template>

<script>
<<<<<<< HEAD
import OrganizationalProfile from "@/components/OrganizationalProfile";
import DocumentCredentialList from "@/components/credentials/DocumentCredentialList";

import { EventBus } from "../main";
export default {
    name: "PublicProfile",
    props: {},
    components: {
        OrganizationalProfile,
        DocumentCredentialList,
    },
    computed: {
        document() {
            return this.$store.getters.organizationalProfile;
        },
        isBusy() {
            return this.$store.getters.isBusy;
        },
        publicDocumentsAndCredentials() {
            console.log(this.$store.getters.publicDocumentsAndCredentials);
            return this.$store.getters.publicDocumentsAndCredentials;
        },
=======
import OganizationalProfile from "@/components/OrganizationalProfile";
import DocumentCredentialList from "@/components/profile/DocumentCredentialList";

import { EventBus } from "../main";
export default {
  name: "PublicProfile",
  props: {},
  components: {
    OganizationalProfile,
    DocumentCredentialList
  },
  computed: {
    document() {
      return this.$store.getters.organizationalProfile;
>>>>>>> 42a77aa... feture/017 Public Profile List. Datetime Format
    },
    created() {
        EventBus.$emit("title", "Public Profile");
        this.$store.dispatch("loadDocuments");
        this.$store.dispatch("loadCredentials");
    },
};
</script>
