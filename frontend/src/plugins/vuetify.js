import "@mdi/font/css/materialdesignicons.css";
import Vue from "vue";
import Vuetify from "vuetify";
import "vuetify/dist/vuetify.min.css";

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

const uiColor = localStorage.getItem("uiColor");
if (uiColor) {
    primaryColor = uiColor;
}

export default new Vuetify({
    icons: {
        iconfont: "mdiSvg"
    },
    theme: {
        themes: {
            light: {
                primary: primaryColor
            }
        }
    }
});
