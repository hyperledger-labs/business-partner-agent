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
        Add new Business Partner
      </v-card-title>
      <v-container>
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
        <v-row class="mx-2" v-if="partnerLoaded">
          <v-col cols="4">
            <p class="grey--text text--darken-2 font-weight-medium">
              Set a name
            </p>
          </v-col>
          <v-col cols="8">
            <v-text-field
              label="Name"
              placeholder=""
              v-model="alias"
              outlined
              dense
            >
            </v-text-field>
          </v-col>
        </v-row>
        <Profile v-if="partnerLoaded" v-bind:partner="partner" />
      </v-container>
      <v-card-actions>
        <v-layout justify-space-between>
          <v-bpa-button color="secondary" to="/app/partners">Cancel</v-bpa-button>

          <v-bpa-button v-if="!partnerLoaded" color="primary" @click="lookup()"
            >Lookup Partner</v-bpa-button
          >
          <v-bpa-button v-else color="primary" @click="addPartner()"
            >Add Partner</v-bpa-button
          >
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import Profile from "@/components/Profile";
import { getPartnerName } from "../utils/partnerUtils";
import { EventBus } from "../main";
import VBpaButton from "@/components/BpaButton";
export default {
  name: "AddPartner",
  components: {
    VBpaButton,
    Profile,
  },
  created: () => {},
  data: () => {
    return {
      partnerLoading: false,
      partnerLoaded: false,
      msg: "",
      did: "",
      alias: "",
      partner: {},
    };
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
              this.alias = getPartnerName(partner);
              if ({}.hasOwnProperty.call(partner, "credential"))
                this.partnerLoaded = true;
            } else if (partner.ariesSupport) {
              // Todo I need to know if I'm in aries mode to allow connection using aries
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
          // EventBus.$emit("error", e);
        });
    },
    addPartner() {
      let partnerToAdd = {
        did: `${this.did}`,
      };

      if (this.alias && this.alias !== "") {
        partnerToAdd.alias = this.alias;
      }
      this.$axios
        .post(`${this.$apiBaseUrl}/partners`, partnerToAdd)
        .then((result) => {
          console.log(result);

          if (result.status === 201) {
            //   this.$axios.get(`${this.$apiBaseUrl}/partners/${result.data.id}`).then( res => {
            //       console.log(res);
            //       this.partnerLoaded = true
            //       this.partnerLoading = false

            //   });
            // } else {
            //   this.partnerLoading = false;
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
  },
};
</script>
