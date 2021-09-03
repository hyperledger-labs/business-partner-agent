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
package org.hyperledger.bpa.impl.aries;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.acy_py.generated.model.V20CredExRecord;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v2.V2IssueIndyCredentialEvent;
import org.hyperledger.aries.api.message.BasicMessage;
import org.hyperledger.aries.api.message.PingEvent;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.webhook.EventHandler;
import org.hyperledger.bpa.impl.ChatMessageManager;
import org.hyperledger.bpa.impl.IssuerCredentialManager;

import java.util.Optional;

@Slf4j
@Singleton
public class AriesEventHandler extends EventHandler {

    private final ConnectionManager conMgmt;

    private final Optional<PingManager> pingMgmt;

    private final HolderCredentialManager holderMgr;

    private final IssuerCredentialManager issuerMgr;

    private final ProofEventHandler proofMgmt;

    private final ChatMessageManager chatMessageManager;

    @Inject
    public AriesEventHandler(
            ConnectionManager conMgmt,
            Optional<PingManager> pingMgmt,
            HolderCredentialManager holderMgr,
            ProofEventHandler proofMgmt,
            IssuerCredentialManager issuerMgr,
            ChatMessageManager chatMessageManager) {
        this.conMgmt = conMgmt;
        this.pingMgmt = pingMgmt;
        this.holderMgr = holderMgr;
        this.issuerMgr = issuerMgr;
        this.proofMgmt = proofMgmt;
        this.chatMessageManager = chatMessageManager;
    }

    @Override
    public void handleConnection(ConnectionRecord connection) {
        log.debug("Connection Event: {}", connection);
        synchronized (conMgmt) {
            if (!connection.isIncomingConnection()) {
                conMgmt.handleOutgoingConnectionEvent(connection);
            } else {
                if (connection.isNotConnectionInvitation()) {
                    conMgmt.handleIncomingConnectionEvent(connection);
                } else if (connection.isOOBInvitation()) {
                    conMgmt.handleOOBInvitation(connection);
                }
            }
        }
    }

    @Override
    public void handlePing(PingEvent ping) {
        pingMgmt.ifPresent(mgmt -> mgmt.handlePingEvent(ping));
    }

    @Override
    public void handleProof(PresentationExchangeRecord proof) {
        log.debug("Present Proof Event: {}", proof);
        synchronized (proofMgmt) {
            proofMgmt.dispatch(proof);
        }
    }

    @Override
    public void handleCredential(V1CredentialExchange v1CredEx) {
        log.debug("Credential Event: {}", v1CredEx);
        // holder events
        if (v1CredEx.isHolder()) {
            synchronized (holderMgr) {
                if (v1CredEx.isCredentialAcked()) {
                    holderMgr.handleV1CredentialExchangeAcked(v1CredEx);
                }
            }
            // issuer events
        } else if (v1CredEx.isIssuer()) {
            synchronized (issuerMgr) {
                if (v1CredEx.isProposalReceived()) {
                    issuerMgr.handleV1CredentialProposal(v1CredEx);
                } else {
                    issuerMgr.handleV1CredentialExchange(v1CredEx);
                }
            }
        }
    }

    @Override
    public void handleCredentialV2(V20CredExRecord v20Credential) {
        log.debug("Credential V2 Event: {}", v20Credential);
        if (V20CredExRecord.RoleEnum.ISSUER.equals(v20Credential.getRole())) {
            synchronized (issuerMgr) {
                issuerMgr.handleV2CredentialExchange(v20Credential);
            }
        } else if (V20CredExRecord.RoleEnum.HOLDER.equals(v20Credential.getRole())) {
            synchronized (holderMgr) {
                if (V20CredExRecord.StateEnum.CREDENTIAL_RECEIVED.equals(v20Credential.getState())) {
                    holderMgr.handleV2CredentialExchangeReceived(v20Credential);
                } else if (V20CredExRecord.StateEnum.DONE.equals(v20Credential.getState())) {
                    holderMgr.handleV2CredentialExchangeDone(v20Credential);
                }
            }
        }
    }

    @Override
    public void handleIssueCredentialV2Indy(V2IssueIndyCredentialEvent revocationInfo) {
        log.debug("Issue Credential V2 Indy Event: {}", revocationInfo);
        synchronized (issuerMgr) {
            issuerMgr.handleIssueCredentialV2Indy(revocationInfo);
        }
        synchronized (holderMgr) {
            holderMgr.handleIssueCredentialV2Indy(revocationInfo);
        }
    }

    @Override
    public void handleBasicMessage(BasicMessage message) {
        // since basic message handling is so simple (only one way to handle it), let
        // the manager handle it.
        chatMessageManager.handleIncomingMessage(message);
    }

    @Override
    public void handleRaw(String eventType, String json) {
        log.trace(json);
    }
}
