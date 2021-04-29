<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-data-table
      :hide-default-footer="itemsWithIndex.length < 10"
      :headers="headers"
      :items="itemsWithIndex"
      item-key="index"
      :sort-by="['createdAt']"
      :sort-desc="[false]"
      single-select
      @click:row="openItem"
  >
    <template v-slot:[`item.state`]="{ item }">
      <v-icon
          v-if="isItemActive(item)"
          color="green"
      >mdi-check</v-icon
      >
      <span v-else>
        {{ item.state.replace("_", " ") }}
      </span>
    </template>
    <template v-slot:[`item.updatedAt`]="{ item }">
      {{ item.updatedAt | formatDateLong }}
    </template>
    <template v-slot:[`item.createdAt`]="{ item }">
      {{ item.createdAt | formatDateLong }}
    </template>
  </v-data-table>
</template>

<script>

  export default {
    props: {
      items: Array,
      headers: {
        type: Array,
        default: () => [],
      },
      isActiveFn: {
        type: Function,
        default: (item) => (item.state === 'credential_issued' || item.state == 'credential_acked')
      }
    },
    data: () => {
      return {
      };
    },
    computed: {
      // Add an unique index, because elements do not have unique id
      itemsWithIndex: function () {
        return this.items.map((item, index) => ({
          ...item,
          index: index + 1,
        }));
      },
    },
    methods: {
      openItem(item) {
        this.$emit('openItem', item);
      },
      isItemActive(item) {
        return this.isActiveFn(item);
      }
    },
    components: {
    },
  };
</script>
