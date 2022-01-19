/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
const addItem = (collection, id, payload) => {
  return { ...collection, [id]: payload };
};

const removeItem = (collection, id) => {
  if (Object.prototype.hasOwnProperty.call(collection, id)) {
    const temporary = { ...collection };
    delete temporary[id];
    return temporary;
  }
  return collection;
};

const removeItemByMessageInfoLinkId = (collection, linkId) => {
  const temporary = { ...collection };
  const keys = Object.keys(temporary);
  let k;
  for (const [index, key] of keys.entries()) {
    const o = collection[key];
    console.log(`${index} ${key} ${linkId}`);
    console.log(o);
    if (o?.message?.info?.linkId === linkId) {
      k = key;
    }
  }
  if (k) {
    delete temporary[k];
    return temporary;
  }
  return collection;
};

const state = {
  activityNotifications: {},
  credentialNotifications: {},
  partnerNotifications: {},
  presentationNotifications: {},
  taskNotifications: {},
};

const getters = {
  activityNotifications: (state) => {
    return state.activityNotifications;
  },
  activityNotificationsCount: (state) => {
    return Object.keys(state.activityNotifications).length;
  },
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
    const type = payload.message.type;
    const id = payload.message.linkId;
    console.log(`onNotification(type=${type}, id=${id})`);
    switch (type) {
      case "ACTIVITY_NOTIFICATION":
        state.activityNotifications = addItem(
          state.activityNotifications,
          id,
          payload
        );
        break;
      case "ON_CREDENTIAL_ACCEPTED":
      case "ON_CREDENTIAL_ADDED":
      case "ON_CREDENTIAL_OFFERED":
      case "ON_CREDENTIAL_PROBLEM":
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
  activityNotificationSeen(state, payload) {
    const id = payload.id;
    state.activityNotifications = removeItem(state.activityNotifications, id);
  },
  credentialNotificationSeen(state, payload) {
    const id = payload.id;
    state.credentialNotifications = removeItem(
      state.credentialNotifications,
      id
    );
    state.activityNotifications = removeItemByMessageInfoLinkId(
      state.activityNotifications,
      id
    );
  },
  partnerNotificationSeen(state, payload) {
    const id = payload.id;
    state.partnerNotifications = removeItem(state.partnerNotifications, id);
  },
  presentationNotificationSeen(state, payload) {
    const id = payload.id;
    state.presentationNotifications = removeItem(
      state.presentationNotifications,
      id
    );
  },
  taskNotificationSeen(state, payload) {
    const id = payload.id;
    state.taskNotifications = removeItem(state.taskNotifications, id);
  },
  activityNotificationClear(state) {
    state.activityNotification = {};
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
    state.activityNotifications = {};
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
