/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/business-partner-agent

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.hyperledger.oa.impl.aries;

import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.credential.CredentialExchange;
import org.hyperledger.aries.api.message.PingEvent;
import org.hyperledger.aries.api.proof.PresentationExchangeRecord;
import org.hyperledger.aries.webhook.EventHandler;

import javax.inject.Singleton;
import java.util.Optional;

@Slf4j
@Singleton
public class AriesEventHandler extends EventHandler {

    private final ConnectionManager conMgmt;

    private final Optional<PingManager> pingMgmt;

    private final AriesCredentialManager credMgmt;

    private final ProofManager proofMgmt;

    public AriesEventHandler(
            ConnectionManager conMgmt,
            Optional<PingManager> pingMgmt,
            AriesCredentialManager credMgmt,
            ProofManager proofMgmt) {
        this.conMgmt = conMgmt;
        this.pingMgmt = pingMgmt;
        this.credMgmt = credMgmt;
        this.proofMgmt = proofMgmt;
    }

    @Override
    public void handleConnection(ConnectionRecord connection) {
        log.debug("Connection Event: {}", connection);
        conMgmt.handleConnectionEvent(connection);
    }

    @Override
    public void handlePing(PingEvent ping) {
        log.debug("Ping: {}", ping);
        pingMgmt.ifPresent(mgmt -> mgmt.handlePingEvent(ping));
    }

    @Override
    public void handleProof(PresentationExchangeRecord proof) {
        log.debug("Present Proof Event: {}", proof);
        synchronized (proofMgmt) {
            if (proof.isVerified() && "verifier".equals(proof.getRole())
                    || "presentation_acked".equals(proof.getState()) && "prover".equals(proof.getRole())) {
                proofMgmt.handleAckedOrVerifiedProofEvent(proof);
            } else {
                proofMgmt.handleProofEvent(proof);
            }
        }
    }

    @Override
    public void handleCredential(CredentialExchange credential) {
        log.debug("Issue Credential Event: {}", credential);
        // holder events, because I could also be an issuer
        if ("holder".equals(credential.getRole())) {
            synchronized (credMgmt) {
                if ("credential_received".equals(credential.getState())) {
                    credMgmt.handleStoreCredential(credential);
                } else if ("credential_acked".equals(credential.getState())) {
                    credMgmt.handleCredentialAcked(credential);
                } else {
                    credMgmt.handleCredentialEvent(credential);
                }
            }
        }
    }

    @Override
    public void handleRaw(String eventType, String json) {
        log.trace(json);
    }
}
