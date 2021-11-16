<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-data-table
      :hide-default-footer="data.length < 10"
      :loading="isBusy"
      v-model="inputValue"
      :headers="headers"
      :items="data"
      :show-select="selectable"
      single-select
      :sort-by="['createdDate']"
      :sort-desc="[false]"
      @click:row="open"
    >
      <template v-slot:[`item.label`]="{ item }">
        <new-message-icon :type="'credential'" :id="item.id"></new-message-icon>
        <span v-bind:class="{ 'font-weight-medium': item.new }">
          {{ item.label }}
        </span>
      </template>
      <template v-slot:[`item.type`]="{ item }">
        <div
          v-if="
            item.type === CredentialTypes.UNKNOWN.type &&
            item.credentialDefinitionId
          "
          v-bind:class="{ 'font-weight-medium': !item.new }"
        >
          {{ item.credentialDefinitionId | credentialTag | capitalize }}
        </div>

        <div v-else v-bind:class="{ 'font-weight-medium': item.new }">
          {{ item.typeLabel }}
        </div>
      </template>

      <template v-slot:[`item.createdDate`]="{ item }">
        {{ item.createdDate | moment("YYYY-MM-DD HH:mm") }}
      </template>

      <template v-slot:[`item.updatedDate`]="{ item }">
        {{ item.updatedDate | moment("YYYY-MM-DD HH:mm") }}
      </template>

      <template v-slot:[`item.issuedAt`]="{ item }">
        {{ item.issuedAt | moment("YYYY-MM-DD HH:mm") }}
      </template>

      <template v-slot:[`item.revoked`]="{ item }">
        <v-icon
          v-if="item.revocable && item.revoked"
          :title="$t('component.credExList.table.iconCredRevoked')"
          >$vuetify.icons.revoked</v-icon
        >
        <v-icon
          v-else-if="item.revocable && !item.revoked"
          color="green"
          :title="$t('component.credExList.table.holderNotRevoked')"
          >$vuetify.icons.revoke</v-icon
        >
        <v-icon
          v-else
          :title="$t('component.credExList.table.holderNotRevocable')"
          color="green"
          >$vuetify.icons.check</v-icon
        >
      </template>

      <template v-slot:[`item.isPublic`]="{ item }">
        <v-icon v-if="item.isPublic" color="green">
          $vuetify.icons.public
        </v-icon>
        <template v-else>
          <v-icon>$vuetify.icons.private</v-icon>
        </template>
      </template>
    </v-data-table>
  </v-container>
</template>

<script>
import { CredentialTypes } from "../constants";
import { EventBus } from "../main";
import NewMessageIcon from "@/components/NewMessageIcon";

export default {
  props: {
    type: String,
    headers: Array,
    disableVerificationRequest: {
      type: Boolean,
      required: false,
      default: false,
    },
    selectable: {
      type: Boolean,
      default: false,
    },
    indicateNew: {
      type: Boolean,
      default: false,
    },
  },
  components: {
    NewMessageIcon,
  },
  created() {
    this.fetch(this.type);
  },
  data: () => {
    return {
      data: [],
      isBusy: true,
      CredentialTypes: CredentialTypes,
    };
  },
  computed: {
    credentialNotifications() {
      return this.$store.getters.credentialNotifications;
    },
    inputValue: {
      get() {
        return this.value;
      },
      set(val) {
        this.$emit("input", val);
      },
    },
  },
  watch: {
    credentialNotifications: function (newValue) {
      if (newValue && this.type === "credential") {
        // TODO: Don't fetch all partners but only add new credential data
        this.fetch(this.type);
      }
    },
  },
  methods: {
    fetch(type) {
      this.$axios
        .get(`${this.$apiBaseUrl}/wallet/${type}`)
        .then((result) => {
          if ({}.hasOwnProperty.call(result, "data")) {
            this.isBusy = false;

            if (type === "credential") {
              this.data = result.data.filter((item) => {
                return item.issuer;
              });
            } else {
              this.data = result.data;
            }
          }
        })
        .catch((e) => {
          this.isBusy = false;
          if (e.response.status === 404) {
            this.data = [];
          } else {
            EventBus.$emit("error", this.$axiosErrorMessage(e));
          }
        });
    },
    open(doc) {
      console.log("Open Document: ", doc);
      if (this.type === "document") {
        this.$router.push({
          name: "Document",
          params: {
            id: doc.id,
            disableVerificationRequest: this.disableVerificationRequest,
            // type: doc.type,
          },
        });
      } else {
        this.$router.push({
          name: "Credential",
          params: {
            id: doc.id,
            type: doc.type,
          },
        });
      }
    },
  },
};
</script>
