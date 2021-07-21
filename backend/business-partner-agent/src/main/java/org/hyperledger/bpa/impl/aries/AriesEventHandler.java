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

import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.message.PingEvent;
import org.hyperledger.aries.api.message.ProblemReport;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.webhook.EventHandler;
import org.hyperledger.bpa.impl.IssuerManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Slf4j
@Singleton
public class AriesEventHandler extends EventHandler {

    private final ConnectionManager conMgmt;

    private final Optional<PingManager> pingMgmt;

    private final CredentialManager credMgmt;

    private final ProofEventHandler proofMgmt;

    private final IssuerManager issuerMgr;

    @Inject
    public AriesEventHandler(
            ConnectionManager conMgmt,
            Optional<PingManager> pingMgmt,
            CredentialManager credMgmt,
            ProofEventHandler proofMgmt,
            IssuerManager issuerMgr) {
        this.conMgmt = conMgmt;
        this.pingMgmt = pingMgmt;
        this.credMgmt = credMgmt;
        this.proofMgmt = proofMgmt;
        this.issuerMgr = issuerMgr;
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
    public void handleCredential(V1CredentialExchange credential) {
        log.debug("Issue Credential Event: {}", credential);
        // holder events, because I could also be an issuer
        if (CredentialExchangeRole.HOLDER.equals(credential.getRole())) {
            synchronized (credMgmt) {
                if (CredentialExchangeState.CREDENTIAL_ACKED.equals(credential.getState())) {
                    credMgmt.handleCredentialAcked(credential);
                } else {
                    credMgmt.handleCredentialEvent(credential);
                }
            }
        } else if (CredentialExchangeRole.ISSUER.equals(credential.getRole())) {
            synchronized (issuerMgr) {
                issuerMgr.handleCredentialExchange(credential);
            }
        }
    }

    @Override
    public void handleProblemReport(ProblemReport report) {
        // problem reports can happen on several levels, currently we assume that all
        // reports are proof related
        proofMgmt.handleProblemReport(report.getThread().getThid(), report.getDescription());
    }

    @Override
    public void handleRaw(String eventType, String json) {
        log.trace(json);
    }
}
