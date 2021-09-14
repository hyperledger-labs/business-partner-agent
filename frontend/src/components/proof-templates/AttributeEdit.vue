<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <h4 class="pb-5">Data fields</h4>
    <v-row v-show="attributeGroup.ui.selectedAttributes.length === 0">
      <v-col>
        <v-alert type="error"
          >There must be at least one selected data field
        </v-alert>
      </v-col>
    </v-row>
    <v-text-field
      v-model="searchField"
      append-icon="$vuetify.icons.search"
      label="Search"
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
      <template v-slot:item.operator="{ item }">
        <v-select
          :items="operators"
          v-model="item.conditions[0].operator"
          dense
        />
      </template>
      <template v-slot:item.value="{ item }">
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
<script>
import proofTemplateService from "@/services/proofTemplateService";

export default {
  name: "AttributeEdit",
  props: {
    attributeGroup: {},
    attributeGroupHeaders: {
      type: Array,
      default: () => [
        {
          text: "Name",
          value: "name",
        },
        {
          text: "Operator",
          value: "operator",
        },
        {
          text: "Value",
          value: "value",
        },
      ],
    },
  },
  data: () => {
    return {
      operators: [],
      searchField: "",
      rules: {
        onlyInteger: (value) =>
          value === undefined
            ? true
            : /^-?\d+$/.test(value) || "Value is not an integer",
        valueMin: (value) =>
          value >= -2147483648 || "Value must be above -2147483648",
        valueMax: (value) =>
          value <= 2147483647 || "Value must not be above 2147483647",
      },
    };
  },
  mounted() {
    // load condition operators (>, <, ==, etc)
    proofTemplateService.getKnownConditionOperators().then((result) => {
      this.operators.push("", ...result.data);
    });
  },
  methods: {
    setPredicateConditionsErrorCount(event, attributeGroup) {
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
