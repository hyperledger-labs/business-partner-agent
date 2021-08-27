<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card max-width="600" class="mx-auto" flat>
      <v-card-title class="grey--text text--darken-2">
        {{ $t("view.addPartner.title") }}
      </v-card-title>
      <v-container fluid>
        <v-radio-group v-model="radios" row>
          <v-radio value="did" label="By DID" />
          <v-radio value="url" label="By Invitation" />
        </v-radio-group>
      </v-container>

      <v-container v-if="radios==='did'">
        <v-row>
          <v-col cols="12">
            <v-text-field
                prepend-icon="$vuetify.icons.identity"
                label="Decentralized Identifier (DID)"
                placeholder=""
                v-model="did"
                @change="partnerLoaded = false"
                outlined
                dense
            >
            </v-text-field>
          </v-col>
        </v-row>
        <v-row>
          <v-layout justify-center>
            <v-progress-circular
                v-if="partnerLoading"
                indeterminate
                color="primary"
            ></v-progress-circular>
          </v-layout>
        </v-row>

        <v-row>
          <v-layout justify-center>
            <div class="font-weight-medium">{{ msg }}</div>
          </v-layout>
        </v-row>
        <v-row v-if="partnerLoaded">
          <v-col cols="4">
            <v-list-item-title
                class="grey--text text--darken-2 font-weight-medium"
            >
              {{ $t("view.addPartner.setName") }}
            </v-list-item-title>
          </v-col>
          <v-col cols="8">
            <v-text-field
              label="Name"
              persistent-placeholder
              :placeholder="aliasPlaceholder"
              v-model.trim="alias"
              outlined
              dense
            >
            </v-text-field>
          </v-col>
          <v-col cols="4">
            <v-list-item-title
                class="grey--text text--darken-2 font-weight-medium"
            >
              {{ $t("view.addPartner.setTags") }}
            </v-list-item-title>
          </v-col>
          <v-col cols="8">
            <v-autocomplete
                multiple
                v-model="selectedTags"
                :items="tags"
                chips
                deletable-chips
            >
            </v-autocomplete>
          </v-col>
          <v-list-item>
            <v-list-item-title
                class="grey--text text--darken-2 font-weight-medium"
            >{{ $t("view.addPartner.trustPing") }}</v-list-item-title
            >

            <v-list-item-action>
              <v-switch v-model="trustPing"></v-switch>
            </v-list-item-action>
          </v-list-item>
        </v-row>

        <Profile v-if="partnerLoaded" v-bind:partner="partner" />
      </v-container>
      <v-card-actions v-if="radios==='did'">
        <v-layout justify-space-between>
          <v-bpa-button color="secondary" to="/app/partners">
            {{ $t("button.cancel") }}
          </v-bpa-button>

          <v-bpa-button v-if="!partnerLoaded" color="primary" @click="lookup()">
            {{ $t("view.addPartner.lookupPartnerBtnLabel") }}
          </v-bpa-button>
          <v-bpa-button v-else color="primary" @click="addPartner()"
            >{{ $t("view.addPartner.addPartnerBtnLabel") }}
          </v-bpa-button>
        </v-layout>
      </v-card-actions>

      <v-container v-if="radios==='url'">
        <v-row>
          <v-col cols="12">
            <v-text-field
                prepend-icon="$vuetify.icons.invitation"
                label="Invitation URL"
                placeholder=""
                v-model="invitationUrl"
                @change="invitationUrlLoaded = false"
                outlined
                dense
            >
            </v-text-field>
          </v-col>
        </v-row>
        <v-row>
          <v-layout justify-center>
            <v-progress-circular
                v-if="invitationUrlLoading"
                indeterminate
                color="primary"
            ></v-progress-circular>
          </v-layout>
        </v-row>
        <v-row v-if="invitationUrlLoaded">
          <v-col cols="4">
            <v-list-item-title
                class="grey--text text--darken-2 font-weight-medium"
            >
              {{ $t("view.addPartner.setName") }}
            </v-list-item-title>
          </v-col>
          <v-col cols="8">
            <v-text-field
                label="Name"
                persistent-placeholder
                :placeholder=aliasPlaceholder
                v-model.trim="alias"
                outlined
                dense
            >
            </v-text-field>
          </v-col>
          <v-col cols="4">
            <v-list-item-title
                class="grey--text text--darken-2 font-weight-medium"
            >
              {{ $t("view.addPartner.setTags") }}
            </v-list-item-title>
          </v-col>
          <v-col cols="8">
            <v-autocomplete
                multiple
                v-model="selectedTags"
                :items="tags"
                chips
                deletable-chips
            >
            </v-autocomplete>
          </v-col>
          <v-list-item>
            <v-list-item-title
                class="grey--text text--darken-2 font-weight-medium"
            >{{ $t("view.addPartner.trustPing") }}</v-list-item-title
            >

            <v-list-item-action>
              <v-switch v-model="trustPing"></v-switch>
            </v-list-item-action>
          </v-list-item>
        </v-row>
      </v-container>
      <v-card-actions v-if="radios==='url'">
        <v-layout justify-space-between>
          <v-bpa-button color="secondary" to="/app/partners">
            {{ $t("button.cancel") }}
          </v-bpa-button>
          <v-bpa-button v-if="!invitationUrlLoaded" color="primary" @click="checkInvitation()" :disabled="invitationUrl===''">Check Invitation</v-bpa-button>
          <v-bpa-button v-else color="primary" @click="acceptInvitation()">Accept Invitation</v-bpa-button>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import Profile from "@/components/Profile";
import { EventBus } from "@/main";
import VBpaButton from "@/components/BpaButton";
import store from "@/store";

export default {
  name: "AddPartner",
  components: {
    VBpaButton,
    Profile,
  },
  created() {},
  data: () => {
    return {
      partnerLoading: false,
      partnerLoaded: false,
      msg: "",
      did: "",
      alias: "",
      aliasPlaceholder: "",
      partner: {},
      search: "",
      selectedTags: [],
      trustPing: true,
      invitationUrl: "",
      invitationUrlLoaded: false,
      invitationUrlLoading: false,
      receivedInvitation: {},
      radios: "did"
    };
  },
  computed: {
    tags() {
      return this.$store.state.tags
        ? this.$store.state.tags.map((tag) => tag.name)
        : [];
    },
  },
  methods: {
    lookup() {
      this.msg = "";
      this.partnerLoading = true;
      this.$axios
        .get(`${this.$apiBaseUrl}/partners/lookup/${this.did}`)
        .then((result) => {
          this.partnerLoading = false;
          console.log(result);
          if (
            {}.hasOwnProperty.call(result, "status") &&
            result.status === 200
          ) {
            let partner = result.data;
            if ({}.hasOwnProperty.call(partner, "credential")) {
              this.partner = partner;
              this.aliasPlaceholder = partner.name;
              if ({}.hasOwnProperty.call(partner, "credential"))
                this.partnerLoaded = true;
            } else if (partner.ariesSupport) {
              this.msg =
                "Partner has no public profile. You can add him to get more information using Aries protocols.";
              this.partnerLoaded = true;
            } else {
              this.msg = "Partner has no public profile and no Aries support.";
            }
          }
        })
        .catch((e) => {
          console.error(e);
          this.msg = `Could not resolve ${this.did}.`;
          this.partnerLoading = false;
          EventBus.$emit("error", e);
        });
    },
    addPartner() {
      let partnerToAdd = {
        did: `${this.did}`,
      };

      if (this.alias && this.alias !== "") {
        partnerToAdd.alias = this.alias;
      }

      partnerToAdd.tag = this.$store.state.tags.filter((tag) => {
        return this.selectedTags.includes(tag.name);
      });

      partnerToAdd.trustPing = this.trustPing;
      this.$axios
        .post(`${this.$apiBaseUrl}/partners`, partnerToAdd)
        .then((result) => {
          if (result.status === 201) {
            store.dispatch("loadPartners");
            EventBus.$emit("success", "Partner added successfully");
            this.$router.push({
              name: "Partners",
            });
          }
        })
        .catch((e) => {
          if (e.response.status === 412) {
            EventBus.$emit("error", "Partner already exists");
          } else {
            console.error(e);
            EventBus.$emit("error", e);
          }
        });
    },
    checkInvitation() {
      this.msg = "";
      this.alias = "";
      this.receivedInvitation = {};
      this.invitationUrlLoaded = false;
      if (this.invitationUrl) {
        this.invitationUrlLoading = true;
        let request = {
          invitationUrl: encodeURIComponent(this.invitationUrl)
        };

        this.$axios
          .post(`${this.$apiBaseUrl}/invitations/check`, request)
          .then((result) => {
            this.invitationUrlLoading = false;
            this.receivedInvitation = Object.assign({}, result.data);
            this.invitationUrlLoaded = this.receivedInvitation.invitationBlock != null;
            // add in their label as the default alias for adding
            this.aliasPlaceholder = this.receivedInvitation.label;
            //this.alias = this.receivedInvitation.label;
          })
        .catch((e) => {
          console.error(e);
          this.invitationUrlLoading = false;
          EventBus.$emit("error", e);
        });
      }
    },
    acceptInvitation() {
      if (this.invitationUrlLoaded) {
        // build up our accept request using the invitation block from check and user entered data...
        let request = {
          invitationBlock: this.receivedInvitation.invitationBlock
        };
        if (this.alias && this.alias !== "") {
          request.alias = this.alias;
        } else if (this.aliasPlaceholder && this.aliasPlaceholder !== "") {
          request.alias = this.aliasPlaceholder;
        }
        request.tag = this.$store.state.tags.filter((tag) => {
          return this.selectedTags.includes(tag.name);
        });
        request.trustPing = this.trustPing;

        // send if off and add a new partner
        this.$axios
          .post(`${this.$apiBaseUrl}/invitations/accept`, request)
          .then(() => {
            store.dispatch("loadPartners");
            this.receivedInvitation = {};
            this.invitationUrlLoaded = false;
            EventBus.$emit("success", "Partner added successfully");
            this.$router.push({
              name: "Partners",
            });
          })
          .catch((e) => {
            console.error(e);
            EventBus.$emit("error", e);
          });
      }
    },
  },
};
</script>
