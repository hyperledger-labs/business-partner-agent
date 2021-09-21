const addItem = (collection, id, payload) => {
  return { ...collection, [id]: payload };
};

const removeItem = (collection, id) => {
  if ({}.hasOwnProperty.call(collection, id)) {
    const tmp = { ...collection };
    delete tmp[id];
    return tmp;
  }
  return collection;
};

const state = {
  credentialNotifications: {},
  partnerNotifications: {},
  presentationNotifications: {},
  taskNotifications: {},
};

const getters = {
  credentialNotifications: (state) => {
    return state.credentialNotifications;
  },
  credentialNotificationsCount: (state) => {
    return Object.keys(state.credentialNotifications).length;
  },
  partnerNotifications: (state) => {
    return state.partnerNotifications;
  },
  partnerNotificationsCount: (state) => {
    return Object.keys(state.partnerNotifications).length;
  },
  presentationNotifications: (state) => {
    return state.presentationNotifications;
  },
  presentationNotificationsCount: (state) => {
    return Object.keys(state.presentationNotifications).length;
  },
  taskNotifications: (state) => {
    return state.taskNotifications;
  },
  taskNotificationsCount: (state) => {
    return Object.keys(state.taskNotifications).length;
  },
};

const actions = {};

const mutations = {
  onNotification(state, payload) {
    let type = payload.message.type;
    let id = payload.message.linkId;
    console.log(`onNotification(type=${type}, id=${id})`);
    switch (type) {
      case "ON_CREDENTIAL_ADDED":
        state.credentialNotifications = addItem(
          state.credentialNotifications,
          id,
          payload
        );
        break;
      case "ON_PARTNER_REMOVED":
        state.partnerNotifications = removeItem(
          state.partnerNotifications,
          payload.message.partner.id
        );
        break;
      case "ON_PARTNER_REQUEST_COMPLETED":
      case "ON_PARTNER_REQUEST_RECEIVED":
      case "ON_PARTNER_ACCEPTED":
      case "ON_PARTNER_ADDED":
        state.partnerNotifications = addItem(
          state.partnerNotifications,
          payload.message.partner.id,
          payload.message.partner
        );
        break;
      case "ON_PRESENTATION_PROVED":
      case "ON_PRESENTATION_VERIFIED":
      case "ON_PRESENTATION_REQUEST_RECEIVED":
      case "ON_PRESENTATION_REQUEST_SENT":
        state.presentationNotifications = addItem(
          state.presentationNotifications,
          id,
          payload
        );
        state.partnerNotifications = addItem(
          state.partnerNotifications,
          payload.message.partner.id,
          payload.message.partner
        );
        break;
      case "TASK_ADDED":
        state.taskNotifications = addItem(state.taskNotifications, id, payload);
        break;
      case "TASK_COMPLETED":
        state.taskNotifications = removeItem(state.taskNotifications, id);
        break;
      default:
        console.log(`Unknown notification type: ${type}`);
    }
  },
  credentialNotificationSeen(state, payload) {
    let id = payload.id;
    state.credentialNotifications = removeItem(
      state.credentialNotifications,
      id
    );
  },
  partnerNotificationSeen(state, payload) {
    let id = payload.id;
    state.partnerNotifications = removeItem(state.partnerNotifications, id);
  },
  presentationNotificationSeen(state, payload) {
    let id = payload.id;
    state.presentationNotifications = removeItem(
      state.presentationNotifications,
      id
    );
  },
  taskNotificationSeen(state, payload) {
    let id = payload.id;
    state.taskNotifications = removeItem(state.taskNotifications, id);
  },
  credentialNotificationsClear(state) {
    state.credentialNotifications = {};
  },
  partnerNotificationsClear(state) {
    state.partnerNotifications = {};
  },
  presentationNotificationsClear(state) {
    state.presentationNotifications = {};
  },
  taskNotificationsClear(state) {
    state.taskNotifications = {};
  },
  allNotificationsClear(state) {
    state.credentialNotifications = {};
    state.partnerNotifications = {};
    state.presentationNotifications = {};
    state.taskNotifications = {};
  },
};

export default {
  state,
  getters,
  actions,
  mutations,
};
