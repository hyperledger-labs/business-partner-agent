<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <chat-window
      :rooms="rooms"
      :rooms-loaded="roomsLoaded"
      :room-id="roomId"
      :messages="messages"
      :fetch-messages="fetchMessages"
      :messages-loaded="messagesLoaded"
      @send-message="sendMessage"
      :show-add-room="false"
      :show-audio="false"
      :show-files="false"
      :show-emojis="false"
  />
</template>

<script>
  import ChatWindow from 'vue-advanced-chat'
  import 'vue-advanced-chat/dist/vue-advanced-chat.css'
  import * as partnerUtils from "@/utils/partnerUtils";
  import partnerService from "@/services/partnerService";

  export default {
    components: {
      ChatWindow
    },
    created() { },
    data() {
      return {
        rooms: [],
        roomsLoaded: true,
        roomId: null,
        messages: [],
        messagesLoaded: true
      }
    },
    computed: {
      partners() {
        return this.$store.getters.getPartners;
      },
      partnerMessages() {
        return this.$store.getters.getPartnerMessages;
      },
    },
    watch: {
      partners(val) {
        this.roomsLoaded = false;
        const _rooms = [];
        for (let i = 0; i < val.length; i++) {
          const p = val[i];
          _rooms.push({ roomId: p.id, roomName: partnerUtils.getPartnerName(p), users: [] });
        }
        this.rooms = _rooms;
        this.roomId = _rooms.length ? _rooms[0].roomId : null;
        this.roomsLoaded = true;
      },
      partnerMessages(val) {
        this.messagesLoaded = false;
        const _msgs = [];
        for (let i = 0; i < val.length; i++) {
          const msg = val[i];

          _msgs.push({
            _id: msg.messageId,
            content: msg.content,
            senderId: msg.partnerId,
          });
        }
        this.messages = _msgs;
        this.messagesLoaded = true;
      },
    },

    methods: {
      // eslint-disable-next-line no-unused-vars
      fetchMessages({ room, options }) {
        this.messages = [];
        this.messagesLoaded = true;
      },
      // eslint-disable-next-line no-unused-vars
      async sendMessage({ content, roomId, file, replyMessage }) {
        // we are sending content to roomId (partner)...
        await partnerService.sendMessage(roomId, content);
      }
    }
  }
</script>
