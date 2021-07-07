// style.scss imports our font awesome styles and material design styles
import Vue from "vue";
import Vuetify from "vuetify";

import {
  mdiViewDashboard,
  mdiForumOutline,
  mdiAccountCircle,
  mdiBriefcaseVariant,
  mdiFingerprint,
  mdiWallet,
  mdiCog,
  mdiChevronLeft,
  mdiDelete,
  mdiContentCopy,
  mdiPlus,
  mdiPencil,
  mdiChevronRight,
  mdiCheck,
  mdiRefresh,
  mdiImport,
  mdiEye,
  mdiEyeOff,
  mdiBrightness1,
  mdiShapePolygonPlus,
  mdiInformationOutline,
  mdiQrcode,
  mdiGlobeModel,
  mdiLogout,
  mdiHandshake,
  mdiAlertCircleOutline,
  mdiClockTimeThreeOutline,
  mdiContentSave,
} from "@mdi/js";

Vue.use(Vuetify);

export default new Vuetify({
  defaultAssets: {
    font: true,
    icons: "md",
  },
  icons: {
    iconfont: "fa",
    values: {
      user: mdiAccountCircle,
      identity: mdiFingerprint,
      dashboard: mdiViewDashboard,
      profile: mdiGlobeModel,
      wallet: mdiWallet,
      connections: mdiBriefcaseVariant,
      partners: mdiHandshake,
      notifications: mdiForumOutline,
      settings: mdiCog,
      signout: mdiLogout,
      about: mdiInformationOutline,
      prev: mdiChevronLeft,
      next: mdiChevronRight,
      delete: mdiDelete,
      copy: mdiContentCopy,
      add: mdiPlus,
      pencil: mdiPencil,
      fingerprint: mdiFingerprint,
      check: mdiCheck,
      refresh: mdiRefresh,
      import: mdiImport,
      public: mdiEye,
      private: mdiEyeOff,
      partnerState: mdiBrightness1,
      newMessage: mdiShapePolygonPlus,
      qrCode: mdiQrcode,
      credentialManagement: "fas fa-file-signature",
      proofRequests: "fas fa-exchange-alt",
      connectionAlert: mdiAlertCircleOutline,
      connectionWaiting: mdiClockTimeThreeOutline,
      cancel: "fas fa-times-circle",
      save: mdiContentSave,
    },
  },
  theme: {
    // dark will be set via configuration from the backend (bpa.ux.theme.dark), this is set in App.vue
    dark: false,
    options: {
      customProperties: true,
    },
    // themes will be set via configuration from the backend (bpa.ux.theme.themes), these are set in App.vue
    // all the following properties must be set in the configuration
    themes: {
      light: {
        // this are the default vuetify light values
        // primary is a customized color...
        primary: "#4A148C",
        secondary: "#424242",
        accent: "#82B1FF",
        error: "#FF5252",
        info: "#2196F3",
        success: "#4CAF50",
        warning: "#FFC107",
        // the following are custom and are used as variables in style.scss
        bgLight: "#FAFAFA",
        bgLight2: "#ECECEC",
        font: "#313132",
        anchor: "#1A5A96",
        anchorHover: "#3B99FC",
      },
    },
  },
});
