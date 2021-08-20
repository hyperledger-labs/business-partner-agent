<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    Requested Information: {{ record.proofTemplateInfo.proofTemplate.name }}
    <attribute-group
      v-if="record.proofTemplateInfo && record.proofTemplateInfo.proofTemplate"
      v-bind:data="record.proofTemplateInfo.proofTemplate.attributeGroups"
    >
    </attribute-group>
  </v-container>
</template>

<script>
import AttributeGroup from "@/components/proof-templates/AttributeGroup";

export default {
  name: "PresentationRecord",
  props: {
    record: Object,
  },
  mounted() {},
  methods: {},
  data: () => {
    return {};
  },
  components: {
    AttributeGroup,
  },
};
</script>

<style scoped>
.v-btn {
  margin-left: 10px;
}
</style>
