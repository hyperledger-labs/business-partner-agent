/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { IStateNotifications } from "@/store/state-type";

const addItem = (collection: any, id: any, payload: any) => {
  return { ...collection, [id]: payload };
};

const removeItem = (collection: any, id: any) => {
  if (Object.prototype.hasOwnProperty.call(collection, id)) {
    const temporary = { ...collection };
    delete temporary[id];
    return temporary;
  }
  return collection;
};

const removeItemByMessageInfoLinkId = (collection: any, linkId: any) => {
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

const state: IStateNotifications = {
  activityNotifications: {},
  credentialNotifications: {},
  partnerNotifications: {},
  presentationNotifications: {},
  taskNotifications: {},
};

const getters = {
  activityNotifications: (state: IStateNotifications) => {
    return state.activityNotifications;
  },
  activityNotificationsCount: (state: IStateNotifications) => {
    return Object.keys(state.activityNotifications).length;
  },
  credentialNotifications: (state: IStateNotifications) => {
    return state.credentialNotifications;
  },
  credentialNotificationsCount: (state: IStateNotifications) => {
    return Object.keys(state.credentialNotifications).length;
  },
  partnerNotifications: (state: IStateNotifications) => {
    return state.partnerNotifications;
  },
  partnerNotificationsCount: (state: IStateNotifications) => {
    return Object.keys(state.partnerNotifications).length;
  },
  presentationNotifications: (state: IStateNotifications) => {
    return state.presentationNotifications;
  },
  presentationNotificationsCount: (state: IStateNotifications) => {
    return Object.keys(state.presentationNotifications).length;
  },
  taskNotifications: (state: IStateNotifications) => {
    return state.taskNotifications;
  },
  taskNotificationsCount: (state: IStateNotifications) => {
    return Object.keys(state.taskNotifications).length;
  },
};

const actions = {};

const mutations = {
  onNotification(state: IStateNotifications, payload: any) {
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
  activityNotificationSeen(state: IStateNotifications, payload: any) {
    const id = payload.id;
    state.activityNotifications = removeItem(state.activityNotifications, id);
  },
  credentialNotificationSeen(state: IStateNotifications, payload: any) {
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
  partnerNotificationSeen(state: IStateNotifications, payload: any) {
    const id = payload.id;
    state.partnerNotifications = removeItem(state.partnerNotifications, id);
  },
  presentationNotificationSeen(state: IStateNotifications, payload: any) {
    const id = payload.id;
    state.presentationNotifications = removeItem(
      state.presentationNotifications,
      id
    );
  },
  taskNotificationSeen(state: IStateNotifications, payload: any) {
    const id = payload.id;
    state.taskNotifications = removeItem(state.taskNotifications, id);
  },
  activityNotificationClear(state: IStateNotifications) {
    state.activityNotifications = {};
  },
  credentialNotificationsClear(state: IStateNotifications) {
    state.credentialNotifications = {};
  },
  partnerNotificationsClear(state: IStateNotifications) {
    state.partnerNotifications = {};
  },
  presentationNotificationsClear(state: IStateNotifications) {
    state.presentationNotifications = {};
  },
  taskNotificationsClear(state: IStateNotifications) {
    state.taskNotifications = {};
  },
  allNotificationsClear(state: IStateNotifications) {
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
