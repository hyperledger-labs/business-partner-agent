/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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
      const basicMessage = payload.message.info;
      const msgs = state.messages ? state.messages : [];
      msgs.push(basicMessage);
      state.messages = msgs;
    },
    markMessagesSeen(state, partnerId) {
      // seen means we remove them from the store for a given partner/room
      const msgs = state.messages ? state.messages : [];
      state.messages = msgs.filter((m) => m.partnerId !== partnerId);
    },
  },
};
