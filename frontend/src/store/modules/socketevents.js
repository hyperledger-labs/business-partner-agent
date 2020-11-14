import Vue from "vue";

const state = {
  newPartners: {},
  newCredentials: {},
  socket: {
    isConnected: false,
    message: "",
    reconnectError: false,
  },
};

const getters = {
  newPartnersCount: () => {
    return Object.keys(state.newPartners).length;
  },
  newPartners: (state) => {
    return state.newPartners;
  },
  newCredentialsCount: () => {
    return Object.keys(state.newCredentials).length;
  },
  newCredentials: (state) => {
    return state.newCredentials;
  },
};

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
  newPartner(state, payload) {
    let id = payload.message.linkId;
    state.newPartners = { ...state.newPartners, [id]: payload };
  },
  newCredential(state, payload) {
    let id = payload.message.linkId;
    state.newCredentials = { ...state.newCredentials, [id]: payload };
  },
  partnerSeen(state, payload) {
    let id = payload.id;
    if ({}.hasOwnProperty.call(state.newPartners, id)) {
      const tmpPartners = { ...state.newPartners };
      delete tmpPartners[id];
      state.newPartners = tmpPartners;
    }
  },
  credentialSeen(state, payload) {
    let id = payload.id;
    if ({}.hasOwnProperty.call(state.newCredentials, id)) {
      const tmpCredentials = { ...state.newCredentials };
      delete tmpCredentials[id];
      state.newCredentials = tmpCredentials;
    }
  },
};

const actions = {
  newPartner({ commit }, payload) {
    commit("newPartner", payload);
    if (payload.message && payload.message.info) {
      payload.message.info.new = true;
      commit("SET_PARTNER", payload.message.info);
    }
  },
  newCredential({ commit }, payload) {
    commit("newCredential", payload);
  },
};

export default {
  state,
  getters,
  actions,
  mutations,
};
