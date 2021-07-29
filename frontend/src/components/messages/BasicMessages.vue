<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <chat-window
      :current-user-id="currentUserId"
      :responsive-breakpoint="responsiveBreakpoint"
      :rooms="rooms"
      :rooms-loaded="roomsLoaded"
      :room-id="roomId"
      :messages="messages"
      :messages-loaded="messagesLoaded"
      :message-actions="messageActions"
      @fetch-messages="fetchMessages"
      @send-message="sendMessage"
      :show-add-room="false"
      :show-audio="false"
      :show-files="false"
      :show-emojis="false"
      :show-reaction-emojis="false"
      :show-new-messages-divider="false"
      :text-formatting="false"
      :text-messages="textMessages"
  />
</template>

<script>
  import ChatWindow from 'vue-advanced-chat'
  import 'vue-advanced-chat/dist/vue-advanced-chat.css'
  import * as partnerUtils from "@/utils/partnerUtils";
  import partnerService from "@/services/partnerService";
  import { mapMutations } from 'vuex';
  import {CHAT_CURRENT_USERID} from "@/constants";
  import {formatDateLong} from "@/filters";

  export default {
    components: {
      ChatWindow
    },
    created() {
    },
    data() {
      return {
        currentUserId: CHAT_CURRENT_USERID,
        responsiveBreakpoint: 9999,
        rooms: [],
        roomsLoaded: true,
        roomId: null,
        messages: [],
        messagesLoaded: true,
        messageActions: [],
        textMessages: {
          CONVERSATION_STARTED: ""
        }
      }
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
      partnersCount(val) {
        console.log(`partnersCount(${val})`);
        this.loadRooms();
      },
      // eslint-disable-next-line no-unused-vars
      messagesCount(val) {
        console.log(`messagesCount(${val})`);
        // reload the current room
        this.fetchMessages({room: { roomId: this.roomId}})
      },
    },

    methods: {
      ...mapMutations([
        'onMessageReceived'
      ]),
      loadRooms() {
        console.log("loadRooms()");
        this.roomsLoaded = false;
        this.rooms = [];

        const _rooms = [];
        const partners = this.$store.getters.getPartners;
        for (let i = 0; i < partners.length; i++) {
          const p = partners[i];
          // assume they have a connection id, but check to make sure this partner is ARIES
          if (p.ariesSupport) {
            const name = partnerUtils.getPartnerName(p);
            // each room is for a single partner/connection
            // so set the room id to the partner id.
            // add users to represent the partner and us.
            const room = {
              roomId: p.id,
              roomName: name,
              users: [
                {
                  _id: p.id,
                  username: name
                },
                {
                  _id: CHAT_CURRENT_USERID,
                  username: "Me"
                }
              ]
            };
            _rooms.push(room);
          }
        }
        _rooms.sort((a,b) => a.roomName.localeCompare(b.roomName))
        this.rooms = _rooms;
        this.roomId = this.roomId === null ? (_rooms.length ? _rooms[0].id : null) : null;
        this.roomsLoaded = true;
      },
      // eslint-disable-next-line no-unused-vars
      fetchMessages({ room, options = {} }) {
        console.log(`fetchMessages(room = ${room.roomId})`);
        this.messagesLoaded = false;
        this.messages = [];
        this.roomId = room.roomId;

        const _msgs = [];
        const allMessages = this.$store.getters.messages.map((x) => x);
        const _pms = allMessages.filter(m => m.partnerId === room.roomId || m.roomId === room.roomId);
        _pms.sort((a, b) => a.time - b.time);

        for (let i = 0; i < _pms.length; i++) {
          const msg = _pms[i];
          _msgs.push({
            _id: msg.messageId,
            content: msg.content,
            senderId: msg.partnerId,
            timestamp: formatDateLong(msg.time),
          });
        }

        // need a slight timeout to allow the lists to load properly
        setTimeout(() => {
          this.messages = _msgs;
          console.log(this.messages);
          this.messagesLoaded = true;
          console.log(this.messagesLoaded);
        }, 250);
      },
      // eslint-disable-next-line no-unused-vars
      async sendMessage({ content, roomId, file, replyMessage }) {
        // add our side of the conversation to the message list...
        // for this, we just use our hardcoded value for partnerId that matches our currentUserId property for the
        // component. this will put our messages on the right hand side of the conversation.
        const payload = {
          message: {
            info: {
              messageId: new Date().getTime().toString(),
              content: content,
              partnerId: CHAT_CURRENT_USERID,
              roomId: roomId
            }
          }
        };
        // store with incoming messages...
        this.onMessageReceived(payload);

        // we are sending content to roomId (partner)...
        await partnerService.sendMessage(roomId, content);
      }
    }
  }
</script>
