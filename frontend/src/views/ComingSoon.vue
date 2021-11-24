<template>
  <v-container>
    <p>{{ $t("view.comingSoon.text") }}</p>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
export default {
  name: "ComingSoon",
  components: {},
  created() {
    EventBus.$emit("title", this.$t("view.comingSoon.title"));
  },
  data: () => {
    return {};
  },
};
</script>
