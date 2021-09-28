<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        {{ $t("view.sendPresentation.title") }}
      </v-card-title>

      <v-card-text>
        <h4 class="pt-4">{{ $t("view.sendPresentation.selectCredential") }}</h4>
        <MyCredentialList
          v-bind:headers="credHeaders"
          type="credential"
          ref="PresentationList"
          selectable
        ></MyCredentialList>
      </v-card-text>

      <v-card-actions>
        <v-layout align-end justify-end>
          <v-bpa-button color="secondary" @click="cancel()">{{
            $t("button.cancel")
          }}</v-bpa-button>
          <v-bpa-button
            :loading="this.isBusy"
            color="primary"
            @click="sendPresentation()"
            >{{ $t("button.submit") }}</v-bpa-button
          >
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "../main";

// import { CredentialTypes } from "../constants";
import MyCredentialList from "@/components/MyCredentialList";
import VBpaButton from "@/components/BpaButton";

export default {
  name: "SendPresentation",
  components: {
    VBpaButton,
    MyCredentialList,
  },
  props: {
    id: String,
  },
  created() {
    EventBus.$emit("title", this.$t("view.sendPresentation.title"));
  },
  data: () => {
    return {
      isBusy: false,
      credHeaders: [
        {
          text: "Label",
          value: "label",
        },
        {
          text: "Type",
          value: "type",
        },
        {
          text: "Issued by",
          value: "issuer",
        },
        {
          text: "Issued at",
          value: "issuedAt",
        },
      ],
    };
  },
  computed: {},
  methods: {
    sendPresentation() {
      this.isBusy = true;
      if (this.$refs.PresentationList.selected.length === 1) {
        if (this.$refs.PresentationList.selected[0].id) {
          let selectedCredential = this.$refs.PresentationList.selected[0].id;
          this.$axios
            .post(`${this.$apiBaseUrl}/partners/${this.id}/proof-send`, {
              myCredentialId: selectedCredential,
            })
            .then((res) => {
              console.log(res);
              this.isBusy = false;
              EventBus.$emit("success", "Presentation sent");
              this.$router.push({
                name: "Partner",
                params: { id: this.id },
              });
            })
            .catch((e) => {
              this.isBusy = false;
              EventBus.$emit("error", this.$axiosErrorMessage(e));
            });
        } else {
          this.isBusy = false;
        }
      } else {
        this.isBusy = false;
        EventBus.$emit("error", "No credential selected");
      }
    },

    cancel() {
      this.$router.go(-1);
    },
  },
};
</script>

<style scoped>
.bg-light {
  background-color: #fafafa;
}

.bg-light-2 {
  background-color: #ececec;
}
</style>
