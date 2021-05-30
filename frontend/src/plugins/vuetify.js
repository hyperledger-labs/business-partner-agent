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
  mdiHandshake, mdiAlertCircleOutline, mdiClockTimeThreeOutline, mdiContentSave
} from '@mdi/js';

Vue.use(Vuetify);

// Theming
let primaryColor = "#4A148C";
if (process.env.VUE_APP_UI_COLOR) {
  primaryColor = process.env.VUE_APP_UI_COLOR;
}

const uiColor = localStorage.getItem("uiColor");
if (uiColor) {
  primaryColor = uiColor;
}

export default new Vuetify({
  defaultAssets: {
    font: true,
    icons: 'md'
  },
  icons: {
    iconfont: 'fa',
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
      credentialManagement:'fas fa-file-signature',
      proofRequests: 'fas fa-exchange-alt',
      connectionAlert: mdiAlertCircleOutline,
      connectionWaiting: mdiClockTimeThreeOutline,
      cancel: 'fas fa-times-circle',
      save: mdiContentSave

    }
  },
  theme: {
    options: {
      customProperties: true
    },
    themes: {
      light: {
        primary: primaryColor
      }
    }
  }
});
