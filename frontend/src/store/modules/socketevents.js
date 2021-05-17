import Vue from "vue";

const state = {
  newPartners: {},
  newCredentials: {},
  newPresentationRequests: {},
  newPresentations: {},

  socket: {
    isConnected: false,
    message: "",
    reconnectError: false,
  },
};

const getters = {
  newPartnerEventsCount: () => {
    return (
      Object.keys(state.newPartners).length +
      Object.keys(state.newPresentationRequests).length +
      Object.keys(state.newPresentations).length
    );
  },
  newPartnersCount: (state) => {
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

  newPresentationRequests: (state) => {
    return state.newPresentationRequests;
  },
  newPresentations: (state) => {
    return state.newPresentations;
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
  newPresentationRequest(state, payload) {
    let id = payload.message.linkId;
    console.log("NEW PRESENTAITON REQUEST");
    state.newProofRequests = {
      ...state.newPresentationRequests,
      [id]: payload,
    };
    console.log(Object.keys(state.newProofRequests).length);
  },
  newPresentation(state, payload) {
    let id = payload.message.linkId;
    state.newPresentations = { ...state.newPresentations, [id]: payload };
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
  presentationRequestSeen(state, payload) {
    let id = payload.id;
    if ({}.hasOwnProperty.call(state.newPresentationRequests, id)) {
      const tmpPresentationRequests = { ...state.newPresentationRequests };
      delete tmpPresentationRequests[id];
      state.newPresentationRequests = tmpPresentationRequests;
    }
  },
  presentationSeen(state, payload) {
    let id = payload.id;
    if ({}.hasOwnProperty.call(state.newPresentations, id)) {
      const tmpProofs = { ...state.newPresentations };
      delete tmpProofs[id];
      state.newPresentations = tmpProofs;
    }
  },
};

const actions = {};

export default {
  state,
  getters,
  actions,
  mutations,
};
