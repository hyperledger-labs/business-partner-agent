<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-data-table
      :loading="isLoading"
      :hide-default-footer="items.length < 10"
      :headers="headers"
      :items="items"
      :sort-by="['updatedAt']"
      :sort-desc="[true]"
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
              !record.canBeFullfilled
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
<script>
import { proofExService } from "@/services";
import { EventBus } from "@/main";
import { PresentationExchangeStates, RequestTypes } from "@/constants";
import NewMessageIcon from "@/components/NewMessageIcon";
import PresentationRecord from "@/components/PresentationRecord";
import PresentationRecordV2 from "@/components/PresentationRecordV2";
import VBpaButton from "@/components/BpaButton";
export default {
  props: {
    value: Array,
    openItemById: String,
  },
  mounted() {
    // Open Item directly. Is used for links from notifications/activity
    if (this.openItemById) {
      // FIXME: items observable is typically not resolved yet. Then items is empty
      const item = this.items.find((item) => item.id === this.openItemById);
      if (item) {
        this.openItem(item);
      } else {
        // Load record separately if items have not be resolved
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
      selected: [],
      record: {},
      dialog: false,
      isBusy: false,
      isLoading: false,
      isWaitingForMatchingCreds: false,
      declineReasonText: "",
      headers: [
        {
          text: "",
          value: "indicator",
          sortable: false,
          filterable: false,
        },
        {
          text: "Name",
          value: "label",
        },
        {
          text: "Role",
          value: "role",
        },
        {
          text: "Updated at",
          value: "updatedAt",
        },
        {
          text: "State",
          value: "state",
        },
      ],
    };
  },
  computed: {
    items: {
      get() {
        return this.value;
      },
      set(val) {
        this.$emit("input", val);
      },
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
        return groupsWithCredentials.flat().reduce((x, y) => x && y);
      } else {
        return false;
      }
    },
  },
  methods: {
    closeDialog() {
      this.declineReasonText = "";
      this.dialog = false;
    },
    async approve() {
      const payload = this.prepareApprovePayload();
      try {
        await proofExService.approveProofRequest(this.record.id, payload);
        EventBus.$emit("success", "Proof has been sent");
        this.closeDialog();
        this.$emit("changed");
      } catch (e) {
        EventBus.$emit("error", this.$axiosErrorMessage(e));
      }
    },
    async decline() {
      try {
        await proofExService.declineProofRequest(
          this.record.id,
          this.declineReasonText
        );
        EventBus.$emit("success", "Presentation request declined");
        this.closeDialog();
        this.$emit("changed");
      } catch (e) {
        EventBus.$emit("error", this.$axiosErrorMessage(e));
      }
    },
    openItem(item) {
      const itemCopy = {};
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

      itemCopy.stateToTimestamp = presentationStateToTimestamp;

      this.record = itemCopy;
      this.dialog = true;
      this.addProofData();
      this.$emit("openItem", item);
    },
    closeItem() {
      this.closeDialog();
      this.record = {};
    },
    isStateVerified(item) {
      return item && item.state === PresentationExchangeStates.VERIFIED;
    },
    async deleteItem() {
      try {
        const resp = await proofExService.deleteProofExRecord(this.record.id);
        if (resp.status === 200) {
          const idx = this.items.findIndex(
            (item) => item.id === this.record.id
          );
          this.items.splice(idx, 1);
          EventBus.$emit("success", "Presentation record deleted");
          this.closeDialog();
        }
      } catch (e) {
        EventBus.$emit("error", this.$axiosErrorMessage(e));
      }
    },
    addProofData() {
      if (
        Object.hasOwnProperty.call(this.record, "proofRequest") &&
        Object.hasOwnProperty.call(this.record, "proofData")
      ) {
        RequestTypes.map((type) => {
          Object.entries(this.record.proofRequest[type]).map(
            ([groupName, group]) => {
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
    prepareApprovePayload() {
      const payload = {
        referents: [],
      };

      RequestTypes.map((type) => {
        Object.entries(this.record.proofRequest[type]).map(
          ([groupName, group]) => {
            console.log(groupName);
            payload.referents.push(
              group.selectedCredential?.credentialInfo?.referent
            );
          }
        );
      });

      return payload;
    },
    // Checks if proof request can be fullfilled
    canBeFullfilled() {
      const canAttrsFullfilled = Object.values(
        this.record.proofRequest.requestedAttributes
      )
        .map((attrGroup) => {
          return (
            Object.hasOwnProperty.call(attrGroup, "matchingCredentials") &&
            attrGroup.matchingCredentials.length > 0
          );
        })
        .reduce((x, y) => {
          return x && y;
        }, true);

      const canPredicatesFullfilled = Object.values(
        this.record.proofRequest.requestedPredicates
      )
        .map((attrGroup) => {
          return attrGroup.matchingCredentials;
        })
        .reduce((x, y) => {
          return x && y;
        }, true);

      return canAttrsFullfilled && canPredicatesFullfilled;
    },
    getMatchingCredentials() {
      this.isWaitingForMatchingCreds = true;
      proofExService.getMatchingCredentials(this.record.id).then((result) => {
        const matchingCreds = result.data;
        // Match to request
        matchingCreds.forEach((cred) => {
          cred.presentationReferents.forEach((c) => {
            const attr = this.record.proofRequest.requestedAttributes[c];
            const pred = this.record.proofRequest.requestedPredicates[c];
            if (attr) {
              if (!Object.hasOwnProperty.call(attr, "matchingCredentials")) {
                attr.matchingCredentials = [];
              }
              const hasMatchingCred = attr.matchingCredentials.some((item) => {
                return (
                  item.credentialInfo.referent === cred.credentialInfo.referent
                );
              });
              if (!hasMatchingCred) {
                attr.matchingCredentials.push(cred);
              }
            } else if (pred) {
              if (!Object.hasOwnProperty.call(pred, "matchingCredentials")) {
                pred.matchingCredentials = [];
              }

              const hasMatchingPred = pred.matchingCredentials.some((item) => {
                return (
                  item.credentialInfo.referent === cred.credentialInfo.referent
                );
              });
              if (!hasMatchingPred) {
                pred.matchingCredentials.push(cred);
              }
            }
          });
        });

        this.record.canBeFullfilled = this.canBeFullfilled();

        this.isWaitingForMatchingCreds = false;
      });
    },
  },
  watch: {
    dialog(visible) {
      if (visible) {
        this.$store.commit("presentationNotificationSeen", {
          id: this.record.id,
        });
        if (this.record.state === "request_received") {
          this.getMatchingCredentials();
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
