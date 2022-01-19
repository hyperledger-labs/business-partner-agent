<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <chat-window
    :current-user-id="currentUserId"
    :responsive-breakpoint="responsiveBreakpoint"
    :rooms="rooms"
    :rooms-loaded="roomsLoaded"
    :messages="messages"
    :messages-loaded="messagesLoaded"
    :message-actions="messageActions"
    :show-new-messages-divider="showNewMessageDivider"
    @fetch-messages="fetchMessages"
    @send-message="sendMessage"
    @toggle-rooms-list="toggleRoomsList"
    :show-add-room="false"
    :show-audio="false"
    :show-files="false"
    :show-emojis="false"
    :show-reaction-emojis="false"
    :text-formatting="{ disabled: true }"
    :text-messages="textMessages"
    :styles="{ room: { backgroundCounterBadge: 'red' } }"
  />
</template>

<script lang="ts">
import ChatWindow from "vue-advanced-chat";
import "vue-advanced-chat/dist/vue-advanced-chat.css";
import partnerService from "@/services/partnerService";
import { mapMutations } from "vuex";
import { CHAT_CURRENT_USERID, PartnerStates } from "@/constants";
import { formatDateLong } from "@/filters";

export default {
  components: {
    ChatWindow,
  },
  mounted() {
    this.loadRooms();
  },
  data() {
    return {
      currentUserId: CHAT_CURRENT_USERID,
      responsiveBreakpoint: 9999,
      rooms: [],
      roomsLoaded: true,
      currentRoomId: undefined,
      messages: [],
      messagesLoaded: true,
      messageActions: [],
      showNewMessageDivider: false,
      textMessages: {
        CONVERSATION_STARTED: "",
      },
    };
  },
  computed: {
    partnersCount() {
      return this.$store.getters.partnersCount;
    },
    messagesCount() {
      return this.$store.getters.messagesCount;
    },
  },
  watch: {
    // eslint-disable-next-line no-unused-vars
    partnersCount(value) {
      console.log(`partnersCount(${value})`);
      this.loadRooms();
    },
    // eslint-disable-next-line no-unused-vars
    messagesCount(value) {
      console.log(`messagesCount(${value})`);
      // update room message counts..
      this.updateRoomCounts();
    },
  },

  methods: {
    ...mapMutations(["onMessageReceived"]),
    async loadRooms() {
      console.log("loadRooms()");
      this.roomsLoaded = false;
      this.rooms = [];
      this.currentRoomId = undefined;
      const _rooms = [];
      const partners = await partnerService.listPartners();
      if (Array.isArray(partners.data)) {
        for (const p of partners.data) {
          // assume they have a connection id, but check to make sure this partner is ARIES
          if (p.ariesSupport && p.state !== PartnerStates.INVITATION.value) {
            const name = p.name;
            // each room is for a single partner/connection
            // so set the room id to the partner id.
            // add users to represent the partner and us.
            const room = {
              roomId: p.id,
              roomName: name,
              unreadCount: this.getUnreadCount(p.id),
              users: [
                {
                  _id: p.id,
                  username: name,
                },
                {
                  _id: CHAT_CURRENT_USERID,
                  username: this.$t("app.chat.usernameDefault"),
                },
              ],
            };
            _rooms.push(room);
          }
        }
      }
      _rooms.sort((a, b) => a.roomName.localeCompare(b.roomName));
      this.rooms = _rooms;
      this.roomsLoaded = true;
    },
    // eslint-disable-next-line no-unused-vars
    async fetchMessages({
      room,
      options,
    }: {
      room: { roomId: string };
      options: { reset: boolean };
    }) {
      // this event is fired twice, bug in the chat component...
      console.log(
        `fetchMessages(room = ${room.roomId}, options = ${options.reset})`
      );
      // don't set and use the component's roomId property, that fires too many reload room/message events.
      // just track the current room/partner id to auto-refresh if we get a new message while this room is open
      if (options && options.reset) {
        this.messagesLoaded = false;
        this.messages = [];
        this.showNewMessageDivider = false;
        const _msgs = [];
        const _pms = await partnerService.getMessages(room.roomId);

        let newMessages = false;
        if (Array.isArray(_pms.data)) {
          for (const message of _pms.data) {
            const _seen = this.markSeen(message.id);
            if (!_seen) {
              newMessages = true;
            }
            _msgs.push({
              _id: message.id,
              content: message.content,
              senderId: message.incoming
                ? message.partner.id
                : CHAT_CURRENT_USERID,
              timestamp: formatDateLong(message.createdAtTs),
              seen: _seen,
            });
          }
        }
        console.log(this.messages);
        this.messagesLoaded = true;
        this.messages = _msgs;
        this.showNewMessageDivider = newMessages;
        console.log(
          `fetchMessages(room = ${room.roomId}, showNewMessageDivider = ${this.showNewMessageDivider})`
        );
        // remove all of this partner/room message ids from the store...
        this.$store.commit("markMessagesSeen", room.roomId);
      }
      this.currentRoomId = room.roomId;
    },

    async sendMessage({ content, roomId, file, replyMessage }) {
      // we are sending content to currentRoomId (partner)...
      await partnerService.sendMessage(roomId, content);
      // reload our messages (will include our persisted message we just sent)
      await this.fetchMessages({
        room: { roomId: roomId },
        options: { reset: true },
      });
    },
    toggleRoomsList({ opened }) {
      // if the room list is open, clear out the room id...
      // we don't want to refresh a room's message list if we are on the partner/room list
      if (opened) {
        this.currentRoomId = undefined;
      }
    },
    markSeen(id) {
      const _unseen = this.$store.getters.messages;
      if (Array.isArray(_unseen)) {
        for (const _message of _unseen) {
          if (_message.messageId === id) {
            return false;
          }
        }
      }
      return true;
    },
    getUnreadCount(id) {
      const _unread = this.$store.getters.messages;
      let _count = 0;
      if (Array.isArray(_unread)) {
        _count = _unread.filter((m) => m.partnerId === id).length;
      }
      return _count;
    },
    updateRoomCounts() {
      let reloadCurrentRoom = false;
      const _rooms = this.rooms;
      for (const [index, _room] of _rooms.entries()) {
        _room.unreadCount = this.getUnreadCount(_room.roomId);
        this.$set(this.rooms, index, _room);

        // if this room is open, and we have a new unread message, refresh the message list...
        if (_room.roomId === this.currentRoomId && _room.unreadCount > 0) {
          reloadCurrentRoom = true;
        }
      }

      if (reloadCurrentRoom) {
        this.fetchMessages({
          room: { roomId: this.currentRoomId },
          options: { reset: true },
        });
      }
    },
  },
};
</script>
