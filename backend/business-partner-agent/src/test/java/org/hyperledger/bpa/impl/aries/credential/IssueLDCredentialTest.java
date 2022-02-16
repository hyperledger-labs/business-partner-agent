/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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
package org.hyperledger.bpa.impl.aries.jsonld;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.NonNull;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.webhook.EventParser;
import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.impl.aries.AriesEventHandler;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.repository.IssuerCredExRepository;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

@MicronautTest
public class IssueLDCredentialTest extends BaseTest {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    IssuerCredExRepository credExRepo;

    @Inject
    AriesEventHandler aeh;

    private final EventParser ep = new EventParser();

    @Test
    void testHandleHolderCredentialEvents() {
        String offerReceived = loader.load("files/v2-ld-credex-holder/01-offer-received.json");
        V20CredExRecord offer = ep.parseValueSave(offerReceived, V20CredExRecord.class).orElseThrow();

        createDefaultPartner(offer.getConnectionId());

        aeh.handleCredentialV2(offer);

        // TODO flow

    }

    @Test
    void testHandleIssuerCredentialEvents() {
        String offerSent = loader.load("files/v2-ld-credex-issuer/01-offer-sent.json");
        String reqReceived = loader.load("files/v2-ld-credex-issuer/02-request-received.json");
        String credIssued = loader.load("files/v2-ld-credex-issuer/03-credential-issued.json");
        String exDone = loader.load("files/v2-ld-credex-issuer/04-done.json");

        V20CredExRecord offer = ep.parseValueSave(offerSent, V20CredExRecord.class).orElseThrow();
        V20CredExRecord received = ep.parseValueSave(reqReceived, V20CredExRecord.class).orElseThrow();
        V20CredExRecord issued = ep.parseValueSave(credIssued, V20CredExRecord.class).orElseThrow();
        V20CredExRecord done = ep.parseValueSave(exDone, V20CredExRecord.class).orElseThrow();

        Partner p = createDefaultPartner(offer.getConnectionId());
        saveCredentialOffer(p, offer);

        aeh.handleCredentialV2(offer);
        aeh.handleCredentialV2(received);
        aeh.handleCredentialV2(issued);
        aeh.handleCredentialV2(done);

        // TODO flow
    }

    @Test
    void testHandleIssuerProposalReceived() {
        String proposalReceived = loader.load("files/v2-ld-credex-issuer-proposal/01-proposal-received.json");
        V20CredExRecord proposal = ep.parseValueSave(proposalReceived, V20CredExRecord.class).orElseThrow();

        createDefaultPartner(proposal.getConnectionId());

        aeh.handleCredentialV2(proposal);

        // TODO flow
    }

    private void saveCredentialOffer(Partner p, V20CredExRecord exRecord) {
        BPACredentialExchange cex = BPACredentialExchange.builder()
                .partner(p)
                .type(CredentialType.JSON_LD)
                .role(CredentialExchangeRole.ISSUER)
                .state(CredentialExchangeState.OFFER_SENT)
                .pushStateChange(CredentialExchangeState.OFFER_SENT, Instant.now())
                .ldCredential(BPACredentialExchange.ExchangePayload.jsonLD(exRecord.resolveLDCredOffer()))
                .credentialExchangeId(exRecord.getCredentialExchangeId())
                .threadId(exRecord.getThreadId())
                .exchangeVersion(ExchangeVersion.V2)
                .build();
        credExRepo.save(cex);
    }

    private Partner createDefaultPartner(@NonNull String connectionId) {
        Partner p = Partner.builder()
                .connectionId(connectionId)
                .did("did:sov:dummy")
                .ariesSupport(Boolean.TRUE)
                .build();
        return partnerRepo.save(p);
    }

}
