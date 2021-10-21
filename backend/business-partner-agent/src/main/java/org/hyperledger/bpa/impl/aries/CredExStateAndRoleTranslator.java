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

import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;

/**
 * @deprecated use org.hyperledger.bpa.impl.aries.CredExStateAndRoleTranslator
 */
@Deprecated
public interface CredExStateAndRoleTranslator {

    CredentialExchangeState getState();

    CredentialExchangeRole getRole();

    default boolean stateIsProposalSent() {
        return CredentialExchangeState.PROPOSAL_SENT.equals(getState());
    }

    default boolean stateIsProposalReceived() {
        return CredentialExchangeState.PROPOSAL_RECEIVED.equals(getState());
    }

    default boolean stateIsOfferSent() {
        return CredentialExchangeState.OFFER_SENT.equals(getState());
    }

    default boolean stateIsOfferReceived() {
        return CredentialExchangeState.OFFER_RECEIVED.equals(getState());
    }

    default boolean stateIsRequestSent() {
        return CredentialExchangeState.REQUEST_SENT.equals(getState());
    }

    default boolean stateIsRequestReceived() {
        return CredentialExchangeState.REQUEST_RECEIVED.equals(getState());
    }

    default boolean stateIsCredentialIssued() {
        return CredentialExchangeState.CREDENTIAL_ISSUED.equals(getState());
    }

    default boolean stateIsCredentialReceived() {
        return CredentialExchangeState.CREDENTIAL_RECEIVED.equals(getState());
    }

    default boolean stateIsCredentialAcked() {
        return CredentialExchangeState.CREDENTIAL_ACKED.equals(getState());
    }

    default boolean stateIsDone() {
        return CredentialExchangeState.DONE.equals(getState());
    }

    default boolean stateIsDeclined() {
        return CredentialExchangeState.DECLINED.equals(getState());
    }

    default boolean stateIsNotDeclined() {
        return !CredentialExchangeState.DECLINED.equals(getState());
    }

    default boolean stateIsProblem() {
        return CredentialExchangeState.PROBLEM.equals(getState());
    }

    default boolean stateIsRevoked() {
        return CredentialExchangeState.REVOKED.equals(getState());
    }

    default boolean roleIsIssuer() {
        return CredentialExchangeRole.ISSUER.equals(getRole());
    }

    default boolean roleIsHolder() {
        return CredentialExchangeRole.HOLDER.equals(getRole());
    }
}
