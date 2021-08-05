import {CHAT_CURRENT_USERID} from "@/constants";

export default {
  state: {
    messages: [],
    messagesReceivedCount: 0,
  },
  getters: {
    messages: (state) => {
      return state.messages;
    },
    messagesCount: (state) => {
      return state.messages.length;
    },
    messagesReceivedCount: (state) => {
      return state.messagesReceivedCount;
    }
  },
  actions: {

  },
  mutations: {
    onMessageReceived(state, payload) {
      let basicMsg = payload.message.info;
      let msgs = state.messages ? state.messages : [];
      basicMsg.time = new Date().getTime(); // use for sorting
      msgs.push(basicMsg);
      state.messages = msgs;
      if (basicMsg.partnerId !== CHAT_CURRENT_USERID) {
        state.messagesReceivedCount = state.messagesReceivedCount + 1;
      }
    },
    messagesReceivedSeen(state) {
      state.messagesReceivedCount = 0;
    }
  },
};
