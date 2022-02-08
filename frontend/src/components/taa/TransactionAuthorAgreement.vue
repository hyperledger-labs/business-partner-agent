<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-row justify="center">
    <v-dialog v-model="showDialog" max-width="640" persistent id="taa">
      <v-form ref="form" v-model="valid" lazy-validation>
        <v-card>
          <v-card-title class="headline">
            {{ $t("component.transactionAuthorAgreement.title") }}
          </v-card-title>
          <v-card-text>
            {{ $t("component.transactionAuthorAgreement.text") }}: <br /><br />
            <hr />
            <br />
            <span v-if="isTaaLoaded()">
              <v-markdown :source="taaText()"></v-markdown>
            </span>
            <span v-else>
              <div class="text-center">
                <v-progress-circular
                  v-if="!isTaaLoaded()"
                  indeterminate
                  color="primary"
                ></v-progress-circular>
                <div>
                  {{ $t("component.transactionAuthorAgreement.loading") }}
                </div>
              </div>
            </span>
            <hr />
            <small v-show="isTaaLoaded()"
              >{{ $t("component.transactionAuthorAgreement.labelVersion") }}:
              {{ getTaaVersion() }}</small
            >
            <v-checkbox
              v-model="agree"
              :rules="[(v) => !!v || $t('app.rules.agree')]"
              :label="$t('component.transactionAuthorAgreement.labelCheckbox')"
              required
            ></v-checkbox>
          </v-card-text>
          <v-card-actions>
            <v-spacer></v-spacer>
            <v-bpa-button :disabled="!valid" color="primary" @click="register">
              {{ $t("button.writeToLedger") }}
            </v-bpa-button>
          </v-card-actions>
        </v-card>
      </v-form>
    </v-dialog>
  </v-row>
</template>
<script lang="ts">
import { mapActions, mapGetters } from "vuex";
import VueMarkdown from "vue-markdown-render";
import VBpaButton from "@/components/BpaButton";

export default {
  props: {},
  components: {
    VBpaButton,
    "v-markdown": VueMarkdown,
  },
  data() {
    return {
      valid: true,
      agree: false,
      showDialog: this.isTaaRequired,
      taaText: this.getTaaText,
    };
  },
  methods: {
    ...mapActions(["isRegistrationRequired", "getTaa", "registerTaa"]),
    ...mapGetters({
      isTaaRequired: "taaRequired",
      getTaaText: "taaText",
      getTaaVersion: "taaVersion",
      isTaaLoaded: "taaLoaded",
    }),
    register() {
      if (this.$refs.form.validate()) {
        this.registerTaa();
      }
    },
  },
};
</script>
