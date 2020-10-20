/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.controller.api;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Websocket events
 */
@Data
@NoArgsConstructor
@RequiredArgsConstructor(staticName = "of")
public class WebSocketMessageBody {

    private long id = System.nanoTime();

    private long timestamp = System.currentTimeMillis();

    @NonNull
    private WebSockerMessage message;

    @Data
    @Builder
    public static final class WebSockerMessage {
        private WebSocketMessageType type;
        private WebSocketMessageState state;
        private String linkId;
        private Object info;
    }

    public enum WebSocketMessageType {
        PROOF,
        CREDENTIAL,
        CONNECTION,
        ;
    }

    public enum WebSocketMessageState {
        RECEIVED,
        UPDATED;
    }
}
