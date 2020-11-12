<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <div>
    <OrganizationalProfile
      v-if="profile"
      v-bind:documentData="profile"
      isReadOnly
    ></OrganizationalProfile>
    <v-container v-for="(item, index) in credentials" v-bind:key="index">
      <v-row>
        <v-col cols="4">
          <span class="grey--text text--darken-2 font-weight-medium">
            <span v-if="item.type === CredentialTypes.OTHER.name">{{
              item.credentialDefinitionId | credentialTag
            }}</span>
            <span v-else>{{ item.type | credentialLabel }}</span>
          </span>
          <div v-if="item.issuer" class="text-caption">
            verified by {{ item.issuer }}
          </div>
        </v-col>
        <v-col>
          <Credential
            v-bind:document="item"
            isReadOnly
            showOnlyContent
          ></Credential>
        </v-col>
      </v-row>
      <v-divider v-if="index < credentials.length - 1"></v-divider>
    </v-container>
  </div>
</template>

<script>
import { CredentialTypes } from "../constants";
import OrganizationalProfile from "@/components/OrganizationalProfile";
import Credential from "@/components/Credential";
import { getPartnerProfile } from "../utils/partnerUtils";
export default {
  components: {
    OrganizationalProfile,
    Credential,
  },
  props: {
    partner: Object,
  },
  created() {},
  data: () => {
    return {
      CredentialTypes: CredentialTypes,
    };
  },
  computed: {
    profile: function () {
      if (this.partner.profile) {
        return this.partner.profile;
      } else {
        return getPartnerProfile(this.partner);
      }
    },
    credentials: function () {
      let credentials = [];

      if ({}.hasOwnProperty.call(this.partner, "credential")) {
        // Show only creds other than OrgProfile in credential list
        credentials = this.partner.credential.filter((cred) => {
          return cred.type !== CredentialTypes.PROFILE.name;
        });

        return credentials;
      } else {
        return [];
      }
    },
  },
  methods: {},
};
</script>
