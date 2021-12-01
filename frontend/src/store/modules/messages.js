/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
export default {
  state: {
    messages: [],
  },
  getters: {
    messages: (state) => {
      return state.messages;
    },
    messagesCount: (state) => {
      return state.messages.length;
    },
  },
  actions: {},
  mutations: {
    onMessageReceived(state, payload) {
      let basicMsg = payload.message.info;
      let msgs = state.messages ? state.messages : [];
      msgs.push(basicMsg);
      state.messages = msgs;
    },
    markMessagesSeen(state, partnerId) {
      // seen means we remove them from the store for a given partner/room
      let msgs = state.messages ? state.messages : [];
      const unseen = msgs.filter((m) => m.partnerId !== partnerId);
      state.messages = unseen;
    },
  },
};
