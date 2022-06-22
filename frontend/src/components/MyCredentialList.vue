<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-data-table
      :hide-default-footer="hideFooter"
      :loading="isBusy"
      v-model="inputValue"
      :headers="headers"
      :items="data"
      :options.sync="options"
      :server-items-length="totalNumberOfElements"
      :show-select="selectable"
      single-select
      sort-by="createdAt"
      sort-desc
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

      <template v-slot:[`item.createdAt`]="{ item }">
        {{ item.createdAt | formatDateLong }}
      </template>

      <template v-slot:[`item.updatedAt`]="{ item }">
        {{ item.updatedAt | formatDateLong }}
      </template>

      <template v-slot:[`item.issuedAt`]="{ item }">
        {{ item.issuedAt | formatDateLong }}
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

<script lang="ts">
import {
  ApiRoutes,
  CredentialExchangeStates,
  CredentialTypes,
} from "@/constants";
import { EventBus } from "@/main";
import NewMessageIcon from "@/components/NewMessageIcon.vue";
import { AriesCredential, MyDocumentAPI, Page, PageOptions } from "@/services";
import { AxiosResponse } from "axios";
import { appAxios } from "@/services/interceptors";
import { RawLocation } from "vue-router";

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
    useIndy: {
      type: Boolean,
      default: false,
    },
    useJsonLd: {
      type: Boolean,
      default: false,
    },
  },
  components: {
    NewMessageIcon,
  },
  data: () => {
    return {
      data: new Array<AriesCredential | MyDocumentAPI>(),
      options: {},
      totalNumberOfElements: 0,
      hideFooter: false,
      search: "",
      isBusy: true,
      CredentialTypes: CredentialTypes,
    };
  },
  computed: {
    queryFilter() {
      const params = PageOptions.toUrlSearchParams(this.options);
      if (this.useIndy) {
        params.append("types", CredentialTypes.INDY.type);
      }
      if (this.useJsonLd) {
        params.append("types", CredentialTypes.JSON_LD.type);
      }
      return params;
    },
    credentialNotifications(): any {
      return this.$store.getters.credentialNotifications;
    },
    inputValue: {
      get() {
        return this.value;
      },
      set(value: AriesCredential | MyDocumentAPI) {
        this.$emit("input", value);
      },
    },
  },
  watch: {
    credentialNotifications: function (newValue: any) {
      if (newValue && this.type === "credential") {
        // TODO: Don't fetch all partners but only add new credential data
        this.fetch();
      }
    },
    options: {
      handler() {
        this.fetch();
      },
    },
  },
  methods: {
    fetch() {
      appAxios()
        .get(`${ApiRoutes.WALLET}/${this.type}`, {
          params: this.queryFilter,
        })
        .then((result: AxiosResponse<Page<any[]>>) => {
          const { itemsPerPage } = this.options;
          this.isBusy = false;
          this.totalNumberOfElements = result.data.totalSize;
          this.hideFooter = this.totalNumberOfElements <= itemsPerPage;
          this.data =
            this.type === "credential"
              ? result.data.content.filter((item: AriesCredential) => {
                  return (
                    item.state ===
                      (CredentialExchangeStates.CREDENTIAL_ACKED as string) ||
                    item.state === (CredentialExchangeStates.DONE as string)
                  );
                })
              : result.data.content;
        })
        .catch((error) => {
          this.isBusy = false;
          if (error.response.status === 404) {
            this.data = [];
          } else {
            EventBus.$emit("error", this.$axiosErrorMessage(error));
          }
        });
    },
    open(documentOrCredential: AriesCredential | MyDocumentAPI) {
      console.log("Open Document:", documentOrCredential);
      if (this.type === "document") {
        const documentLocation: RawLocation = {
          name: "Document",
          params: {
            id: documentOrCredential.id,
            disableVerificationRequest: this.disableVerificationRequest,
            // type: doc.type,
          },
        };

        this.$router.push(documentLocation);
      } else {
        this.$router.push({
          name: "Credential",
          params: {
            id: documentOrCredential.id,
            type: documentOrCredential.type,
          },
        });
      }
    },
  },
};
</script>
