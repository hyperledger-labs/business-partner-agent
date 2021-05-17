<template>
  <v-container>
    <p>This functionality is coming soon.</p>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
export default {
  name: "ComingSoon",
  components: {},
  created() {
    EventBus.$emit("title", "Coming Soon");
  },
  data: () => {
    return {};
  },
  methods: {},
};
</script>
