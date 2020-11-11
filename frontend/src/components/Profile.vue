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
  created() {
    console.log(this.partner);
  },
  data: () => {
    return {
      identityCredential: {
        type: "OTHER",
        credentialDefinitionId: "afafdsfdsf:sdfaf:Commercial Registry Entry",
        issuer: "blubb",
        credentialData: {
          did: "did:x:4323",
          validFrom: "",
          validUntil: "",
          companyName: "A generic Company",
          lastEntryDate: "",
          nominalCapital: "",
          authorizedOfficers: "",
          registrationNumber: "23434",
          companyAddressStreet: "Uhlsteinstrasse 23",
          companyAddressCountry: "Germany",
          companyAddressLocality: "Berlin",
          companyAddressPostalCode: "2323",
        },
      },
      CredentialTypes: CredentialTypes,
    };
  },
  computed: {
    profile: function () {
      if (this.partner.profile) {
        return this.partner.profile;
      } else {
        console.log(this.partner);
        console.log(getPartnerProfile(this.partner));
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

        if (this.identityCredential) {
          credentials.push(this.identityCredential);
        }

        return credentials;
      } else {
        return [];
      }
    },
  },
  methods: {},
};
</script>
