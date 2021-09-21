import Vue from "vue";

const state = {
  socket: {
    isConnected: false,
    message: "",
    reconnectError: false,
  },
};

const getters = {};

const mutations = {
  SOCKET_ONOPEN(state, event) {
    console.log(event);
    Vue.prototype.$socket = event.currentTarget;
    state.socket.isConnected = true;
  },
  SOCKET_ONCLOSE(state, event) {
    console.log(event);
    state.socket.isConnected = false;
  },
  SOCKET_ONERROR(state, event) {
    console.error(state, event);
  },
  // default handler called for all methods
  SOCKET_ONMESSAGE(state, message) {
    console.log(message);
    state.socket.message = message;
  },
  // mutations for reconnect methods
  SOCKET_RECONNECT(state, count) {
    console.info(state, count);
  },
  SOCKET_RECONNECT_ERROR(state) {
    state.socket.reconnectError = true;
  },
};

const actions = {};

export default {
  state,
  getters,
  actions,
  mutations,
};
