<template>
  <v-row justify="center">
    <v-dialog v-model="showDialog" max-width="640" persistent id="taa">
      <v-form ref="form" v-model="valid" lazy-validation>
        <v-card>
          <v-card-title class="headline">
            Transaction Author Agreement
          </v-card-title>
          <v-card-text>
            To allow other companies to communicate with this agent, some
            configuration has to be written to the ledger - the endpoint
            configuration. In order to do so, the transaction author has to
            accept the Transaction Author Agreement for this session which is
            the following:
            <br /><br />
            <hr>
            <br />
            <span v-if="isTaaLoaded()">
              <v-markdown :source="taaText()"></v-markdown>
            </span>
            <span v-else>
              <div class="text-center">
                  <v-progress-circular v-if="!isTaaLoaded()" indeterminate color="primary"></v-progress-circular>
                  <div>Loading</div>
                </div>
            </span>
            <hr>
            <small v-show="isTaaLoaded()">Version: {{ getTaaVersion() }}</small>
            <v-checkbox
              v-model="agree"
              :rules="[(v) => !!v || 'You must agree to continue!']"
              label="As transaction author I reviewed and accept the above Transaction Author Agreement for this session."
              required
            ></v-checkbox>
          </v-card-text>
          <v-card-actions>
            <v-spacer></v-spacer>
            <v-btn :disabled="!valid" color="primary" @click="register">
              Write endpoints to ledger
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-form>
    </v-dialog>
  </v-row>
</template>
<script>
import { mapActions, mapGetters } from "vuex";
import VueMarkdown from "vue-markdown-render";

export default {
  props: {},
  components: {
    "v-markdown": VueMarkdown,
  },
  data() {
    return {
      valid: true,
      agree: false,
      showDialog: this.isTaaRequired,
      taaText: this.getTaaText
    };
  },
  methods: {
    ...mapActions(["isRegistrationRequired", "getTaa", "registerTaa"]),
    ...mapGetters({
      isTaaRequired: "taaRequired",
      getTaaText: "taaText",
      getTaaVersion: "taaVersion",
      isTaaLoaded: "taaLoaded"
    }),
    register() {
      if (this.$refs.form.validate()) {
        this.registerTaa();
      }
    },
  },
};
</script>
