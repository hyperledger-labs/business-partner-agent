/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.bpa.controller.api;

import lombok.*;

import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.api.aries.AriesCredential;
import org.hyperledger.bpa.api.aries.AriesProof;

/**
 * Websocket events
 */
@Data
@NoArgsConstructor
@RequiredArgsConstructor(staticName = "of")
public class WebSocketMessageBody {

    private final long id = System.nanoTime();

    private final long timestamp = System.currentTimeMillis();

    @NonNull
    private WebSocketMessage message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static final class WebSocketMessage {
        private WebSocketMessageType type;
        private WebSocketMessageState state;
        private String linkId;
        private Object info;
    }

    public enum WebSocketMessageType {
        CONNECTION_REQUEST,
        CREDENTIAL,
        PARTNER,
        PROOF,
        PROOFREQUEST,
    }

    public enum WebSocketMessageState {
        RECEIVED,
        UPDATED,
        SENT
    }

    public static WebSocketMessageBody partnerConnectionRequest(PartnerAPI partner) {
        return WebSocketMessageBody.of(WebSocketMessage
                .builder()
                .type(WebSocketMessageType.CONNECTION_REQUEST)
                .state(WebSocketMessageState.RECEIVED)
                .linkId(partner.getId())
                .info(partner)
                .build());
    }

    public static WebSocketMessageBody partnerReceived(PartnerAPI partner) {
        return WebSocketMessageBody.of(WebSocketMessage
                .builder()
                .type(WebSocketMessageType.PARTNER)
                .state(WebSocketMessageState.RECEIVED)
                .linkId(partner.getId())
                .info(partner)
                .build());
    }

    public static WebSocketMessageBody credentialReceived(AriesCredential credential) {
        return WebSocketMessageBody.of(WebSocketMessage
                .builder()
                .type(WebSocketMessageType.CREDENTIAL)
                .state(WebSocketMessageState.RECEIVED)
                .linkId(credential.getId().toString())
                .info(credential)
                .build());
    }

    public static WebSocketMessageBody proofReceived(AriesProof proof) {
        WebSocketMessageState state;
        if (PresentationExchangeRole.VERIFIER.equals(proof.getRole())) {
            state = WebSocketMessageState.RECEIVED;
        } else {
            state = WebSocketMessageState.SENT;
        }
        return WebSocketMessageBody.of(WebSocketMessage
                .builder()
                .type(WebSocketMessageType.PROOF)
                .state(state)
                .linkId(proof.getId().toString())
                .info(proof)
                .build());
    }

    public static WebSocketMessageBody proofRequestReceived(PresentationExchangeRecord presentationExchangeRecord) {
        WebSocketMessageState state;
        if (PresentationExchangeRole.VERIFIER.equals(presentationExchangeRecord.getRole())) {
            state = WebSocketMessageState.RECEIVED;
        } else {
            state = WebSocketMessageState.SENT;
        }
        return WebSocketMessageBody.of(WebSocketMessage
                .builder()
                .type(WebSocketMessageType.PROOFREQUEST)
                .state(state)
                .linkId(presentationExchangeRecord.getPresentationExchangeId().toString())
                .info(presentationExchangeRecord)
                .build());
    }



}
