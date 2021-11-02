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
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v2.V2IssueIndyCredentialEvent;
import org.hyperledger.aries.api.issue_credential_v2.V2ToV1IndyCredentialConverter;
import org.hyperledger.aries.api.message.BasicMessage;
import org.hyperledger.aries.api.trustping.PingEvent;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof_v2.V20PresExRecord;
import org.hyperledger.aries.api.present_proof_v2.V20PresExRecordToV1Converter;
import org.hyperledger.aries.webhook.EventHandler;
import org.hyperledger.aries.api.ExchangeVersion;
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
            if (connection.isOutgoingConnection()) {
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
    public void handleProofV2(V20PresExRecord v2) {
        log.debug("Present Proof V2 Event: {}", v2);
        handleProof(V20PresExRecordToV1Converter.toV1(v2));
    }

    @Override
    public void handleCredential(V1CredentialExchange v1CredEx) {
        log.debug("Credential Event: {}", v1CredEx);
        // holder events
        if (v1CredEx.roleIsHolder()) {
            synchronized (holderMgr) {
                if (v1CredEx.stateIsCredentialAcked()) {
                    holderMgr.handleV1CredentialExchangeAcked(v1CredEx);
                } else if (v1CredEx.stateIsOfferReceived()) {
                    holderMgr.handleOfferReceived(v1CredEx, ExchangeVersion.V1);
                } else {
                    holderMgr.handleStateChangesOnly(
                            v1CredEx.getCredentialExchangeId(), v1CredEx.getState(),
                            v1CredEx.getUpdatedAt(), v1CredEx.getErrorMsg());
                }
            }
            // issuer events
        } else if (v1CredEx.roleIsIssuer()) {
            synchronized (issuerMgr) {
                if (v1CredEx.stateIsProposalReceived()) {
                    issuerMgr.handleCredentialProposal(v1CredEx, ExchangeVersion.V1);
                } else {
                    issuerMgr.handleV1CredentialExchange(v1CredEx);
                }
            }
        }
    }

    @Override
    public void handleCredentialV2(V20CredExRecord v2CredEx) {
        log.debug("Credential V2 Event: {}", v2CredEx);
        if (v2CredEx.roleIsIssuer()) {
            synchronized (issuerMgr) {
                if (v2CredEx.stateIsProposalReceived()) {
                    issuerMgr.handleCredentialProposal(v2CredEx.toV1CredentialExchangeFromProposal(),
                            ExchangeVersion.V2);
                } else {
                    issuerMgr.handleV2CredentialExchange(v2CredEx);
                }
            }
        } else if (v2CredEx.roleIsHolder()) {
            synchronized (holderMgr) {
                if (v2CredEx.stateIsOfferReceived()) {
                    holderMgr.handleOfferReceived(
                            V2ToV1IndyCredentialConverter.INSTANCE().toV1Offer(v2CredEx), ExchangeVersion.V2);
                } else if (v2CredEx.stateIsDone()) {
                    holderMgr.handleV2CredentialDone(v2CredEx);
                } else {
                    holderMgr.handleStateChangesOnly(
                            v2CredEx.getCredExId(), v2CredEx.getState(),
                            v2CredEx.getUpdatedAt(), v2CredEx.getErrorMsg());
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
