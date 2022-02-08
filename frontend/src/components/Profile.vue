<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <div>
    <v-card class="my-4" v-if="profile">
      <v-card-title class="bg-light">{{
        $t("component.profile.organizationalProfile.title")
      }}</v-card-title>
      <OrganizationalProfile
        v-model="profile"
        v-if="profile"
        isReadOnly
      ></OrganizationalProfile>
      <v-card-actions>
        <!-- this should be protected by roles and whether we are showing the edit button -->
        <v-layout v-if="organizationProfileEditVisible" align-end justify-end>
          <v-bpa-button color="secondary" @click="editProfile">{{
            $t("component.profile.organizationalProfile.edit")
          }}</v-bpa-button>
        </v-layout>
      </v-card-actions>
    </v-card>
    <v-card class="my-4" v-for="item in credentials" v-bind:key="item.id">
      <v-card-title class="bg-light"
        ><span v-if="item.type === CredentialTypes.UNKNOWN.type">{{
          item.credentialDefinitionId | credentialTag
        }}</span>
        <span v-else>{{ item.typeLabel }}</span></v-card-title
      >
      <v-container>
        <v-row>
          <v-col cols="4">
            <v-row
              v-if="item.issuer && profile && profile.id !== item.issuer"
              class="text-caption mt-1 ml-1"
            >
              {{ $t("component.profile.credential.verifiedByLabel") }}
              {{ item.issuer }}
            </v-row>
            <v-row
              v-if="item.credentialData && item.credentialData.validFrom"
              class="mt-1 ml-1 pt-1"
            >
              <v-icon small class="mt-1 ml-1 mr-2">{{ validFrom }}</v-icon>
              <span class="text-caption mt-1">{{
                item.credentialData.validFrom | formatDate
              }}</span>
            </v-row>
            <v-row
              v-if="item.credentialData && item.credentialData.validUntil"
              class="mt-1 ml-1 pt-1"
            >
              <v-icon small class="mt-1 ml-1 mr-2">{{ validUntil }}</v-icon>
              <span class="text-caption mt-1">{{
                item.credentialData.validUntil | formatDate
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
      </v-container>
    </v-card>
    <v-card v-if="!profile && credentials.length === 0" height="100px" flat>
      <v-container fill-height fluid text-center>
        <v-row align="center" justify="center">
          <v-col>
            <h4 class="grey--text">
              {{ $t("component.profile.noProfileMsg") }}
            </h4>
          </v-col>
        </v-row>
      </v-container>
    </v-card>
  </div>
</template>

<script lang="ts">
import { CredentialTypes } from "@/constants";
import OrganizationalProfile from "@/components/OrganizationalProfile.vue";
import Credential from "@/components/Credential.vue";
import {
  getPartnerProfile,
  getPartnerProfileRoute,
} from "@/utils/partnerUtils";
import { mdiCalendarCheck, mdiCalendarRemove } from "@mdi/js";
import VBpaButton from "@/components/BpaButton";

export default {
  components: {
    OrganizationalProfile,
    Credential,
    VBpaButton,
  },
  props: {
    partner: Object,
    organizationProfileEditVisible: {
      type: Boolean,
      default: false,
    },
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
      return getPartnerProfile(this.partner);
    },
    credentials: function () {
      let creds = [];
      if (Object.prototype.hasOwnProperty.call(this.partner, "credential")) {
        creds = this.partner.credential.filter((cred) => {
          if (cred.type !== CredentialTypes.PROFILE.type) {
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
        Object.prototype.hasOwnProperty.call(credential, "credentialData") &&
        typeof credential.credentialData === "object" &&
        credential.credentialData !== null
      ) {
        Object.entries(credential.credentialData).find(([key, value]) => {
          if (key === "id") {
            credential.id = value;
            delete credential.credentialData.id;
          } else if (typeof value === "object" && value !== null) {
            Object.keys(value).map(function (key2) {
              credential.credentialData[key2] = value[key2];
            });
            delete credential.credentialData[key];
          }
        });
      }
      return credential;
    },
    editProfile() {
      const route = getPartnerProfileRoute(this.partner);
      if (route) {
        this.$router.push(route);
      }
    },
  },
};
</script>
