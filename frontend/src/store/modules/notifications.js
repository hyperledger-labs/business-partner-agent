const state = {
  credentialsAdded: {},
  partnerRequestsReceived: {},
  partnersAdded: {},
  partnersRemoved: {},
  presentationsProved: {},
  presentationsVerified: {},
  presentationRequestsReceived: {},
  tasksAdded: {}
};

const getters = {
  credentialsAddedCount: (state) => {
    return Object.keys(state.credentialsAdded).length;
  },
  credentialsAdded: (state) => {
    return state.credentialsAdded;
  },
  partnerRequestsReceivedCount: (state) => {
    return Object.keys(state.partnerRequestsReceived).length;
  },
  partnerRequestsReceived: (state) => {
    return state.partnerRequestsReceived;
  },
  partnersAddedCount: (state) => {
    return Object.keys(state.partnersAdded).length;
  },
  partnersAdded: (state) => {
    return state.partnersAdded;
  },
  partnersRemovedCount: (state) => {
    return Object.keys(state.partnersRemoved).length;
  },
  partnersRemoved: (state) => {
    return state.partnersRemoved;
  },
  presentationsProvedCount: (state) => {
    return Object.keys(state.presentationsProved).length;
  },
  presentationsProved: (state) => {
    return state.presentationsProved;
  },
  presentationsVerifiedCount: (state) => {
    return Object.keys(state.presentationsVerified).length;
  },
  presentationsVerified: (state) => {
    return state.presentationsVerified;
  },
  presentationRequestsReceivedCount: (state) => {
    return Object.keys(state.presentationRequestsReceived).length;
  },
  presentationRequestsReceived: (state) => {
    return state.presentationRequestsReceived;
  },
  tasksAddedCount: (state) => {
    return Object.keys(state.tasksAdded).length;
  },
  tasksAdded: (state) => {
    return state.tasksAdded;
  },
};

const mutations = {
  onNotification(state, payload) {
    let type = payload.message.type;
    let id = payload.message.linkId;
    switch (type) {
      case "onCredentialAdded":
        state.credentialsAdded = { ...state.credentialsAdded, [id]: payload };
        break;
      case "onPartnerRequestReceived":
        state.partnerRequestsReceived = { ...state.partnerRequestsReceived, [id]: payload };
        break;
      case "onPartnerAdded":
        state.partnersAdded = { ...state.partnersAdded, [id]: payload };
        break;
      case "onPartnerRemoved":
        state.partnersRemoved = { ...state.partnersRemoved, [id]: payload };
        break;
      case "onPresentationProved":
        state.presentationsProved = { ...state.presentationsProved, [id]: payload };
        break;
      case "onPresentationVerified":
        state.presentationsVerified = { ...state.presentationsVerified, [id]: payload };
        break;
      case "onPresentationRequestReceived":
        state.presentationRequestsReceived = { ...state.presentationRequestsReceived, [id]: payload };
        break;
      case "onNewTask":
        state.tasksAdded = { ...state.tasksAdded, [id]: payload };
        break;
      default:
        console.log(`Unknown notification type: ${type}`);
    }
  },
};

const actions = {
  credentialsAddedSeen(state, payload) {
    let id = payload.id;
    if ({}.hasOwnProperty.call(state.credentialsAdded, id)) {
      const tmp = { ...state.credentialsAdded };
      delete tmp[id];
      state.credentialsAdded = tmp;
    }
  },
  partnerRequestsReceivedSeen(state, payload) {
    let id = payload.id;
    if ({}.hasOwnProperty.call(state.partnerRequestsReceived, id)) {
      const tmp = { ...state.partnerRequestsReceived };
      delete tmp[id];
      state.partnerRequestsReceived = tmp;
    }
  },
  partnersAddedSeen(state, payload) {
    let id = payload.id;
    if ({}.hasOwnProperty.call(state.partnersAdded, id)) {
      const tmp = { ...state.partnersAdded };
      delete tmp[id];
      state.partnersAdded = tmp;
    }
  },
  partnersRemovedSeen(state, payload) {
    let id = payload.id;
    if ({}.hasOwnProperty.call(state.partnersRemoved, id)) {
      const tmp = { ...state.partnersRemoved };
      delete tmp[id];
      state.partnersRemoved = tmp;
    }
  },
  presentationsReceivedSeen(state, payload) {
    let id = payload.id;
    if ({}.hasOwnProperty.call(state.presentationsReceived, id)) {
      const tmp = { ...state.presentationsReceived };
      delete tmp[id];
      state.presentationsReceived = tmp;
    }
  },
  presentationsVerifiedSeen(state, payload) {
    let id = payload.id;
    if ({}.hasOwnProperty.call(state.presentationsVerified, id)) {
      const tmp = { ...state.presentationsVerified };
      delete tmp[id];
      state.presentationsVerified = tmp;
    }
  },
  presentationRequestsReceivedSeen(state, payload) {
    let id = payload.id;
    if ({}.hasOwnProperty.call(state.presentationRequestsReceived, id)) {
      const tmp = { ...state.presentationRequestsReceived };
      delete tmp[id];
      state.presentationRequestsReceived = tmp;
    }
  },
  tasksAddedSeen(state, payload) {
    let id = payload.id;
    if ({}.hasOwnProperty.call(state.tasksAdded, id)) {
      const tmp = { ...state.tasksAdded };
      delete tmp[id];
      state.tasksAdded = tmp;
    }
  },
  clearTasksAdded() {
    state.tasksAdded = {};
  },
  clearAllNotifications() {
    state.tasksAdded = {};
    state.presentationRequestsReceived = {};
    state.presentationsVerified = {};
    state.presentationsReceived = {};
    state.partnersRemoved = {};
    state.partnersAdded = {};
    state.partnerRequestsReceived = {};
    state.credentialsAdded = {};
  }
};

export default {
  state,
  getters,
  actions,
  mutations,
};
