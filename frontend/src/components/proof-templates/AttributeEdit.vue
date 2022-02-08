<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <h4 class="pb-5">{{ $t("view.proofTemplate.attributes.title") }}</h4>
    <v-row v-show="attributeGroup.ui.selectedAttributes.length === 0">
      <v-col>
        <v-alert type="error"
          >{{ $t("view.proofTemplate.attributes.errorNoAttributes") }}
        </v-alert>
      </v-col>
    </v-row>
    <v-text-field
      v-model="searchField"
      append-icon="$vuetify.icons.search"
      :label="$t('app.search')"
      single-line
      hide-details
      clearable
    ></v-text-field>
    <v-data-table
      show-select
      disable-sort
      :hide-default-footer="attributeGroup.attributes.length < 10"
      :search="searchField"
      :headers="attributeGroupHeaders"
      :items="attributeGroup.attributes"
      v-model="attributeGroup.ui.selectedAttributes"
      item-key="name"
      class="elevation-1"
    >
      <!-- attribute conditions -->
      <template v-slot:[`item.operator`]="{ item }">
        <v-select
          :items="operators"
          v-model="item.conditions[0].operator"
          dense
        />
      </template>
      <template v-slot:[`item.value`]="{ item }">
        <v-text-field
          :type="
            item.conditions[0].operator !== undefined &&
            item.conditions[0].operator !== '' &&
            item.conditions[0].operator !== '=='
              ? 'number'
              : undefined
          "
          :rules="
            item.conditions[0].operator !== undefined &&
            item.conditions[0].operator !== '' &&
            item.conditions[0].operator !== '=='
              ? [rules.onlyInteger, rules.valueMin, rules.valueMax]
              : []
          "
          v-model="item.conditions[0].value"
          v-on:update:error="
            setPredicateConditionsErrorCount($event, attributeGroup)
          "
          dense
        />
      </template>
    </v-data-table>
  </v-container>
</template>
<script lang="ts">
import proofTemplateService from "@/services/proofTemplateService";

export default {
  name: "AttributeEdit",
  props: {
    value: {},
  },
  data: () => {
    return {
      operators: new Array<string>(),
      searchField: "",
    };
  },
  mounted() {
    // load condition operators (>, <, ==, etc)
    proofTemplateService.getKnownConditionOperators().then((result) => {
      this.operators.push("", ...result.data);
    });
  },
  computed: {
    attributeGroup: {
      get() {
        return this.value;
      },
      set(value: any) {
        this.$emit("input", value);
      },
    },
    attributeGroupHeaders() {
      return [
        {
          text: this.$t("view.proofTemplate.attributes.header.name"),
          value: "name",
        },
        {
          text: this.$t("view.proofTemplate.attributes.header.operator"),
          value: "operator",
        },
        {
          text: this.$t("view.proofTemplate.attributes.header.value"),
          value: "value",
        },
      ];
    },
    rules() {
      return {
        onlyInteger: (value: string) =>
          value === undefined
            ? true
            : /^-?\d+$/.test(value) || this.$t("app.rules.onlyInteger"),
        valueMin: (value: number) =>
          value >= -2_147_483_648 ||
          `${this.$t("app.rules.valueMin")} -2147483648`,
        valueMax: (value: number) =>
          value <= 2_147_483_647 ||
          `${this.$t("app.rules.valueMax")} 2147483647`,
      };
    },
  },
  methods: {
    setPredicateConditionsErrorCount(event: boolean, attributeGroup) {
      if (event === true) {
        attributeGroup.ui.predicateConditionsErrorCount += 1;
      } else if (
        attributeGroup.ui.predicateConditionsErrorCount > 0 &&
        event === false
      ) {
        attributeGroup.ui.predicateConditionsErrorCount -= 1;
      }
    },
  },
};
</script>
