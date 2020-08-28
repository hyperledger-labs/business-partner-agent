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
package org.hyperledger.oa.impl.aries;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.credential.CredentialExchange;
import org.hyperledger.aries.api.message.PingEvent;
import org.hyperledger.aries.api.proof.PresentationExchangeRecord;
import org.hyperledger.aries.webhook.EventHandler;

import io.micronaut.websocket.WebSocketBroadcaster;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class AriesEventHandler extends EventHandler {

    @Inject
    private Optional<ConnectionManager> conMgmt;

    @Inject
    private Optional<PingManager> pingMgmt;

    @Inject
    private Optional<AriesCredentialManager> credMgmt;

    @Inject
    private Optional<ProofManager> proofMgmt;

    @Inject
    private WebSocketBroadcaster broadcaster;

    @Override
    public void handleConnection(ConnectionRecord connection) {
        log.debug("Connection Event: {}", connection);
        conMgmt.ifPresent(mgmt -> mgmt.handleConnectionEvent(connection));
    }

    @Override
    public void handlePing(PingEvent ping) {
        log.debug("Ping: {}", ping);
        pingMgmt.ifPresent(mgmt -> mgmt.handlePingEvent(ping));
    }

    @Override
    public void handleProof(PresentationExchangeRecord proof) {
        log.debug("Present Proof Event: {}", proof);
        if (proof.isVerified() && "verifier".equals(proof.getRole())) {
            proofMgmt.ifPresent(mgmt -> mgmt.handleProofEvent(proof));
        }
    }

    @Override
    public void handleCredential(CredentialExchange credential) {
        log.debug("Issue Credential Event: {}", credential);
        // holder events, because I could also be an issuer
        if ("holder".equals(credential.getRole())) {
            if ("credential_received".equals(credential.getState())) {
                credMgmt.ifPresent(mgmt -> mgmt.handleStroreCredential(credential));
            } else if ("credential_acked".equals(credential.getState())) {
                credMgmt.ifPresent(mgmt -> mgmt.handleCredentialAcked(credential));
            } else {
                credMgmt.ifPresent(mgmt -> mgmt.handleCredentialEvent(credential));
            }
        }
    }

    @Override
    public void handleRaw(String eventType, String json) {
        broadcaster.broadcast(json);
    }

}
