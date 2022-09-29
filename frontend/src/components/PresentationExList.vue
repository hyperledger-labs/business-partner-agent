<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-progress-linear
      v-if="isLoadingPresExRecords"
      indeterminate
    ></v-progress-linear>
    <v-data-table
      :hide-default-footer="hideFooter"
      :headers="headers"
      :items="presentationExchangeRecords"
      :options.sync="options"
      :server-items-length="totalNumberOfElements"
      sort-by="updatedAt"
      sort-desc
      single-select
      @click:row="openItem"
    >
      <template v-slot:[`item.indicator`]="{ item }">
        <new-message-icon
          :type="'presentation'"
          :id="item.id"
        ></new-message-icon>
      </template>
      <template v-slot:[`item.label`]="{ item }">
        {{ item.typeLabel }}
      </template>

      <template v-slot:[`item.role`]="{ item }">
        {{ item.role | capitalize }}
      </template>
      <template v-slot:[`item.state`]="{ item }">
        <span>
          {{ (item.state ? item.state.replace("_", " ") : "") | capitalize }}
          <v-icon v-if="item.valid" color="green" class="iconHeight"
            >$vuetify.icons.check</v-icon
          >
          <v-icon
            v-if="isStateVerified(item) && !item.valid && !item.problemReport"
            color="error"
            small
            class="iconHeight"
          >
            $vuetify.icons.connectionAlert
          </v-icon>
          <v-tooltip v-if="item.problemReport" top>
            <template v-slot:activator="{ on, attrs }">
              <v-icon
                color="error"
                small
                class="iconHeight"
                v-bind="attrs"
                v-on="on"
              >
                $vuetify.icons.connectionAlert
              </v-icon>
            </template>
            <span>{{ item.problemReport }}</span>
          </v-tooltip>
        </span>
      </template>
      <template v-slot:[`item.updatedAt`]="{ item }">
        {{ item.updatedAt | formatDateLong }}
      </template>
    </v-data-table>
    <v-dialog v-if="dialog" v-model="dialog" scrollable max-width="1000px">
      <v-card>
        <v-card-title class="bg-light">
          <span class="headline">{{
            $t("component.presentationExList.dialog.title")
          }}</span>
          <v-layout justify-end>
            <v-btn depressed color="red" icon @click="deleteItem">
              <v-icon dark>$vuetify.icons.delete</v-icon>
            </v-btn>
          </v-layout>
        </v-card-title>
        <v-card-text>
          <v-skeleton-loader
            v-if="isWaitingForMatchingCreds"
            type="list-item-three-line"
          />
          <PresentationRecordV2
            class="justify-start"
            v-else-if="showV2"
            v-bind:record="record"
          ></PresentationRecordV2>
          <PresentationRecord
            class="justify-start"
            v-else
            v-bind:record="record"
          ></PresentationRecord>
          <v-alert
            v-if="
              !isWaitingForMatchingCreds &&
              isStateRequestReceived &&
              !record.canBeFulfilled
            "
            dense
            border="left"
            type="warning"
          >
            {{ $t("component.presentationExList.dialog.alertFulfill") }}
          </v-alert>
          <v-text-field
            v-if="isStateRequestReceived"
            v-model="declineReasonText"
            :label="
              $t('component.presentationExList.dialog.declineReasonLabel')
            "
            counter="255"
          ></v-text-field>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-bpa-button color="secondary" @click="closeItem(record)">{{
            $t("button.close")
          }}</v-bpa-button>
          <span v-if="isStateRequestReceived">
            <v-bpa-button color="secondary" @click="decline">{{
              $t("button.decline")
            }}</v-bpa-button>
            <v-bpa-button
              :loading="isBusy"
              color="primary"
              :disabled="!isReadyToApprove"
              @click="approve"
              >{{ $t("button.accept") }}</v-bpa-button
            >
          </span>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>
<style scoped>
.iconHeight {
  display: inherit;
}
</style>
<script lang="ts">
import {
  AriesProofExchange,
  PageOptions,
  partnerService,
  PresentationRequestCredentials,
  proofExService,
  SelectedReferent,
} from "@/services";
import { EventBus } from "@/main";
import {
  CredentialTypes,
  PresentationExchangeRoles,
  PresentationExchangeStates,
  RequestTypes,
} from "@/constants";
import NewMessageIcon from "@/components/NewMessageIcon.vue";
import PresentationRecord from "@/components/PresentationRecord.vue";
import PresentationRecordV2 from "@/components/PresentationRecordV2.vue";
import VBpaButton from "@/components/BpaButton";

export default {
  props: {
    openItemById: String,
    partnerId: String,
  },
  mounted() {
    // Open Item directly. Is used for links from notifications/activity
    if (this.openItemById) {
      // FIXME: items observable is typically not resolved yet. Then items is empty
      const item = this.presentationExchangeRecords.find(
        (item: AriesProofExchange) => item.id === this.openItemById
      );
      if (item) {
        this.openItem(item);
      } else {
        // Load record separately if presentationExchangeRecords have not been resolved
        proofExService.getProofExRecord(this.openItemById).then((resp) => {
          if (resp.data) {
            this.openItem(resp.data);
          }
        });
      }
    }
  },
  data: () => {
    return {
      record: {} as AriesProofExchange & {
        stateToTimestampUiTimeline: [string, number][];
      },
      dialog: false,
      isBusy: false,
      isLoadingPresExRecords: true,
      presentationExchangeRecords: new Array<AriesProofExchange>(),
      options: {},
      totalNumberOfElements: 0,
      hideFooter: false,
      isWaitingForMatchingCreds: false,
      declineReasonText: "",
    };
  },
  watch: {
    dialog(visible: boolean) {
      if (visible) {
        this.$store.commit("presentationNotificationSeen", {
          id: this.record.id,
        });
        if (this.record.state === "request_received") {
          this.getMatchingCredentials();
        }
      }
    },
    options: {
      handler() {
        this.loadPresentationRecords();
      },
    },
  },
  computed: {
    headers() {
      return [
        {
          text: "",
          value: "indicator",
          sortable: false,
          filterable: false,
        },
        {
          text: this.$t("component.presentationExList.table.label"),
          value: "label",
          sortable: false,
        },
        {
          text: this.$t("component.presentationExList.table.role"),
          value: "role",
        },
        {
          text: this.$t("component.presentationExList.table.updatedAt"),
          value: "updatedAt",
        },
        {
          text: this.$t("component.presentationExList.table.state"),
          value: "state",
        },
      ];
    },
    showV2() {
      return (
        this.record.state &&
        (this.record.state === PresentationExchangeStates.PRESENTATION_SENT ||
          this.record.state === PresentationExchangeStates.VERIFIED)
      );
    },
    isStateRequestReceived() {
      return (
        this.record.state &&
        this.record.state === PresentationExchangeStates.REQUEST_RECEIVED
      );
    },
    isReadyToApprove() {
      if (Object.hasOwnProperty.call(this.record, "proofRequest")) {
        const groupsWithCredentials = RequestTypes.map((type) => {
          return Object.values(this.record.proofRequest[type]).map((group) => {
            return Object.hasOwnProperty.call(group, "selectedCredential");
          });
        });
        // eslint-disable-next-line unicorn/no-array-reduce
        return groupsWithCredentials.flat().reduce((x, y) => x && y);
      } else return false;
    },
    typeIsIndy() {
      return this.record.type === CredentialTypes.INDY.type;
    },
  },
  methods: {
    closeDialog() {
      this.declineReasonText = "";
      this.dialog = false;
    },
    async loadPresentationRecords() {
      this.isLoadingPresExRecords = true;
      this.presentationExchangeRecords = [];
      const params = PageOptions.toUrlSearchParams(this.options);
      try {
        const response = await partnerService.getPresentationExRecords(
          this.partnerId,
          params
        );
        const { itemsPerPage } = this.options;
        this.presentationExchangeRecords = response.data.content;
        this.totalNumberOfElements = response.data.totalSize;
        this.hideFooter = this.totalNumberOfElements <= itemsPerPage;
        this.$emit("presRawData", this.presentationExchangeRecords);
      } catch (error) {
        EventBus.$emit("error", this.$axiosErrorMessage(error));
      }
      this.isLoadingPresExRecords = false;
    },
    async approve() {
      const referents = this.prepareReferents();
      try {
        await proofExService.approveProofRequest(this.record.id, referents);
        EventBus.$emit(
          "success",
          this.$t("component.presentationExList.eventSuccessApprove")
        );
        this.closeDialog();
        this.$emit("changed");
      } catch (error) {
        EventBus.$emit("error", this.$axiosErrorMessage(error));
      }
    },
    async decline() {
      try {
        await proofExService.declineProofRequest(
          this.record.id,
          this.declineReasonText
        );
        EventBus.$emit(
          "success",
          this.$t("component.presentationExList.eventSuccessDecline")
        );
        this.closeDialog();
        this.$emit("changed");
      } catch (error) {
        EventBus.$emit("error", this.$axiosErrorMessage(error));
      }
    },
    openItem(
      item: AriesProofExchange & {
        stateToTimestampUiTimeline: [string, number][];
      }
    ) {
      const itemCopy = {} as AriesProofExchange & {
        stateToTimestampUiTimeline: [string, number][];
      };
      Object.assign(itemCopy, item);

      const presentationStateToTimestamp = Object.entries(
        itemCopy.stateToTimestamp
      );

      for (const stateElement of presentationStateToTimestamp) {
        if (
          itemCopy.problemReport &&
          stateElement[0] === PresentationExchangeStates.DECLINED
        ) {
          stateElement.push(itemCopy.problemReport);
        } else {
          stateElement.push(undefined);
        }
      }

      itemCopy.stateToTimestampUiTimeline = presentationStateToTimestamp;

      this.record = itemCopy;
      this.dialog = true;
      this.addProofData();
      this.$emit("openItem", item);
    },
    closeItem() {
      this.closeDialog();
      this.record = {};
    },
    isStateVerified(item: AriesProofExchange) {
      return (
        item &&
        item.role === PresentationExchangeRoles.VERIFIER.toLowerCase() &&
        (item.state === PresentationExchangeStates.VERIFIED.toLowerCase() ||
          item.state === PresentationExchangeStates.DONE.toLowerCase())
      );
    },
    async deleteItem() {
      try {
        const resp = await proofExService.deleteProofExRecord(this.record.id);
        if (resp.status === 200) {
          const index = this.presentationExchangeRecords.findIndex(
            (item: AriesProofExchange) => item.id === this.record.id
          );
          this.presentationExchangeRecords.splice(index, 1);
          EventBus.$emit(
            "success",
            this.$t("component.presentationExList.eventSuccessDelete")
          );
          this.closeDialog();
        }
      } catch (error) {
        EventBus.$emit("error", this.$axiosErrorMessage(error));
      }
    },
    addProofData() {
      if (
        Object.hasOwnProperty.call(this.record, "proofRequest") &&
        Object.hasOwnProperty.call(this.record, "proofData")
      ) {
        RequestTypes.map((type) => {
          Object.entries(this.record.proofRequest[type]).map(
            ([groupName, group]: [string, any]) => {
              if (
                Object.hasOwnProperty.call(this.record.proofData, groupName)
              ) {
                group.proofData = this.record.proofData[groupName];
              }
            }
          );
        });
      }
    },
    prepareReferents(): { [key: string]: SelectedReferent } {
      const referents: { [key: string]: SelectedReferent } = {};

      RequestTypes.map((type) => {
        Object.entries(this.record.proofRequest[type]).map(
          ([groupName, group]: [string, any]) => {
            referents[groupName] = {
              referent: group.selectedCredential?.credentialInfo?.referent,
              revealed: !!group.revealed,
            };
          }
        );
      });

      return referents;
    },
    // Checks if proof request can be fulfilled
    canBeFulfilled() {
      const canAttributesFulfilled = Object.values(
        this.record.proofRequest.requestedAttributes
      )
        .map((attributeGroup: any) => {
          return (
            Object.hasOwnProperty.call(attributeGroup, "matchingCredentials") &&
            attributeGroup.matchingCredentials.length > 0
          );
        })
        // eslint-disable-next-line unicorn/no-array-reduce
        .reduce((x, y) => {
          return x && y;
        }, true);

      const canPredicatesFulfilled = Object.values(
        this.record.proofRequest.requestedPredicates
      )
        .map((attributeGroup: any) => {
          return attributeGroup.matchingCredentials;
        })
        // eslint-disable-next-line unicorn/no-array-reduce
        .reduce((x, y) => {
          return x && y;
        }, true);

      return canAttributesFulfilled && canPredicatesFulfilled;
    },
    getMatchingCredentials() {
      this.isWaitingForMatchingCreds = true;
      if (this.typeIsIndy) {
        proofExService.getMatchingCredentials(this.record.id).then((result) => {
          const matchingCreds: PresentationRequestCredentials[] = result.data;
          // Match to request
          for (const cred of matchingCreds) {
            for (const c of cred.presentationReferents) {
              const attribute = this.record.proofRequest.requestedAttributes[c];
              this.pushIfMatch(attribute, cred);
              // only on anoncred attribute groups
              if (attribute) attribute.revealed = true;
              const pred = this.record.proofRequest.requestedPredicates[c];
              this.pushIfMatch(pred, cred);
            }
          }
          this.record.canBeFulfilled = this.canBeFulfilled();
          this.isWaitingForMatchingCreds = false;
        });
      } else {
        proofExService
          .getMatchingDifCredentials(this.record.id)
          .then((result) => {
            const matches: PresentationRequestCredentials[] = result.data;

            for (const match of matches) {
              const attribute =
                this.record.proofRequest.requestedAttributes[
                  match.credentialInfo.schemaId
                ];
              this.pushIfMatch(attribute, match);
              for (const key of Object.keys(
                this.record.proofRequest.requestedPredicates
              )) {
                if (key.startsWith(match.credentialInfo.schemaId)) {
                  this.pushIfMatch(
                    this.record.proofRequest.requestedPredicates[key],
                    match
                  );
                }
              }
            }
            this.record.canBeFulfilled = result.data.length > 0;
            this.isWaitingForMatchingCreds = false;
          });
      }
    },
    pushIfMatch(
      attributeOrPredicate: any,
      match: PresentationRequestCredentials
    ): void {
      if (attributeOrPredicate) {
        if (
          !Object.hasOwnProperty.call(
            attributeOrPredicate,
            "matchingCredentials"
          )
        ) {
          attributeOrPredicate.matchingCredentials = [];
        }
        const hasMatchingCred = attributeOrPredicate.matchingCredentials.some(
          (item: PresentationRequestCredentials) => {
            return (
              item.credentialInfo.referent === match.credentialInfo.referent
            );
          }
        );
        if (!hasMatchingCred) {
          attributeOrPredicate.matchingCredentials.push(match);
        }
      }
    },
  },
  components: {
    NewMessageIcon,
    PresentationRecord,
    PresentationRecordV2,
    VBpaButton,
  },
};
</script>
