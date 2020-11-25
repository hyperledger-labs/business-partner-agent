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
    <v-container v-for="(item, index) in credentials" v-bind:key="item.id">
      <v-row>
        <v-col cols="4">
          <v-row>
            <span class="grey--text text--darken-2 font-weight-medium">
              <span v-if="item.type === CredentialTypes.OTHER.name">{{
                item.credentialDefinitionId | credentialTag
              }}</span>
              <span v-else>{{ item.type | credentialLabel }}</span>
            </span>
          </v-row>
          <v-row v-if="item.issuer" class="text-caption">
            verified by {{ item.issuer }}
          </v-row>
          <v-row v-if="item.credentialData && item.credentialData.validFrom">
            <v-icon small class="pt-1 mr-2">{{ validFrom }}</v-icon>
            <span class="text-caption">{{
              item.credentialData.validFrom | moment("YYYY-MM-DD")
            }}</span>
          </v-row>
          <v-row v-if="item.credentialData && item.credentialData.validUntil">
            <v-icon small class="pt-1 mr-2">{{ validUntil }}</v-icon>
            <span class="text-caption">{{
              item.credentialData.validUntil | moment("YYYY-MM-DD")
            }}</span>
          </v-row>
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
import { mdiCalendarCheck } from "@mdi/js";
import { mdiCalendarRemove } from "@mdi/js";

export default {
  components: {
    OrganizationalProfile,
    Credential,
  },
  props: {
    partner: Object,
  },
  created() {
    console.log("Partner: ", this.partner);
  },
  data: () => {
    return {
      CredentialTypes: CredentialTypes,
      validFrom: mdiCalendarCheck,
      validUntil: mdiCalendarRemove,
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
      let creds = [];
      if ({}.hasOwnProperty.call(this.partner, "credential")) {
        creds = this.partner.credential.filter((cred) => {
          if (cred.type !== CredentialTypes.PROFILE.name) {
            return this.prepareCredential(cred);
          }
        });
      }
      return creds;
    },
  },
  methods: {
    prepareCredential(credential) {
      if (
        {}.hasOwnProperty.call(credential, "credentialData") &&
        typeof credential.credentialData === "object" &&
        credential.credentialData !== null
      ) {
        Object.entries(credential.credentialData).find(([key, value]) => {
          if (key === "id") {
            credential.id = value;
            delete credential.credentialData.id;
          } else if (typeof value === "object" && value !== null) {
            Object.keys(value).map(function (key) {
              credential.credentialData[key] = value[key];
            });
            delete credential.credentialData[key];
          }
        });
      }
      return credential;
    },
  },
};
</script>
