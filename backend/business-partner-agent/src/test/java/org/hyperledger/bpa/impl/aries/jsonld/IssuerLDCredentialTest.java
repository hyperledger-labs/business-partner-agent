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
import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.issue_credential_v2.V20CredBoundOfferRequest;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V2CredentialExchangeFree;
import org.hyperledger.aries.webhook.EventParser;
import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.controller.api.issuer.CredentialOfferRequest;
import org.hyperledger.bpa.controller.api.issuer.IssueCredentialRequest;
import org.hyperledger.bpa.impl.aries.AriesEventHandler;
import org.hyperledger.bpa.impl.aries.credential.IssuerManager;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.repository.IssuerCredExRepository;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@MicronautTest
@ExtendWith(MockitoExtension.class)
@Disabled
public class IssuerLDCredentialTest extends BaseTest {

    private final String schemaId = "https://w3id.org/citizenship/v1";

    @Inject
    IssuerManager issuer;

    @Inject
    SchemaService schemaService;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    IssuerCredExRepository credExRepo;

    @Inject
    AriesEventHandler aeh;

    @Inject
    Converter conv;

    @Inject
    AriesClient ac;

    private final EventParser ep = new EventParser();

    @Test
    void testHandleIssuerSendCredential() throws IOException {
        String offerSent = loader.load("files/v2-ld-credex-issuer/01-offer-sent.json");
        String reqReceived = loader.load("files/v2-ld-credex-issuer/02-request-received.json");
        String credIssued = loader.load("files/v2-ld-credex-issuer/03-credential-issued.json");
        String exDone = loader.load("files/v2-ld-credex-issuer/04-done.json");

        V20CredExRecord offer = ep.parseValueSave(offerSent, V20CredExRecord.class).orElseThrow();
        V20CredExRecord received = ep.parseValueSave(reqReceived, V20CredExRecord.class).orElseThrow();
        V20CredExRecord issued = ep.parseValueSave(credIssued, V20CredExRecord.class).orElseThrow();
        V20CredExRecord done = ep.parseValueSave(exDone, V20CredExRecord.class).orElseThrow();

        Mockito.when(ac.walletDidPublic()).thenReturn(Optional.of(DID.builder()
                .did("did:indy:1234").method(DID.MethodEnum.SOV).build()));
        Mockito.when(ac.issueCredentialV2Send(Mockito.any(V2CredentialExchangeFree.class)))
                .thenReturn(Optional.of(offer));

        SchemaAPI schemaAPI = createDefaultSchema();
        Partner p = createDefaultPartner(offer.getConnectionId());

        issuer.issueCredential(IssueCredentialRequest.IssueLDCredentialRequest.builder()
                .schemaId(schemaAPI.getId())
                .partnerId(p.getId())
                .document(conv.mapToNode(Map.of("name", "555", "identifier", "1234")))
                .type(CredentialType.JSON_LD)
                .build());

        aeh.handleCredentialV2(offer);
        BPACredentialExchange ex = loadCredEx(offer.getCredentialExchangeId());
        Assertions.assertTrue(ex.stateIsOfferSent());
        Assertions.assertTrue(ex.typeIsJsonLd());
        Assertions.assertEquals(ExchangeVersion.V2, ex.getExchangeVersion());
        Assertions.assertEquals(2, ex.credentialAttributesToMap().size());
        Assertions.assertEquals("555", ex.credentialAttributesToMap().get("name"));

        aeh.handleCredentialV2(received);
        ex = credExRepo.findById(ex.getId()).orElseThrow();
        Assertions.assertTrue(ex.stateIsRequestReceived());

        aeh.handleCredentialV2(issued);
        ex = credExRepo.findById(ex.getId()).orElseThrow();
        Assertions.assertTrue(ex.stateIsCredentialIssued());

        aeh.handleCredentialV2(done);
        ex = credExRepo.findById(ex.getId()).orElseThrow();
        Assertions.assertTrue(ex.stateIsDone());
    }

    @Test
    void testHandleIssuerSendCredentialHolderDeclines() throws IOException {
        String offerSent = loader.load("files/v2-ld-credex-issuer/01-offer-sent.json");
        String abandoned = loader.load("files/v2-ld-credex-issuer/05-abandoned.json");

        V20CredExRecord offer = ep.parseValueSave(offerSent, V20CredExRecord.class).orElseThrow();
        V20CredExRecord problem = ep.parseValueSave(abandoned, V20CredExRecord.class).orElseThrow();

        Mockito.when(ac.walletDidPublic()).thenReturn(Optional.of(DID.builder()
                .did("did:indy:1234").method(DID.MethodEnum.SOV).build()));
        Mockito.when(ac.issueCredentialV2Send(Mockito.any(V2CredentialExchangeFree.class)))
                .thenReturn(Optional.of(offer));

        SchemaAPI schemaAPI = createDefaultSchema();
        Partner p = createDefaultPartner(offer.getConnectionId());

        issuer.issueCredential(IssueCredentialRequest.IssueLDCredentialRequest.builder()
                .schemaId(schemaAPI.getId())
                .partnerId(p.getId())
                .document(conv.mapToNode(Map.of("name", "555", "identifier", "1234")))
                .type(CredentialType.JSON_LD)
                .build());

        aeh.handleCredentialV2(offer);
        BPACredentialExchange ex = loadCredEx(offer.getCredentialExchangeId());
        Assertions.assertTrue(ex.stateIsOfferSent());

        aeh.handleCredentialV2(problem);
        ex = loadCredEx(offer.getCredentialExchangeId());
        Assertions.assertTrue(ex.stateIsProblem());
        Assertions.assertEquals("issuance-abandoned: my reason2", ex.getErrorMsg());
    }

    @Test
    void testHandleIssuerProposalReceived() throws IOException {
        String proposalReceived = loader.load("files/v2-ld-credex-issuer-proposal/01-proposal-received.json");
        String counterOffer = loader.load("files/v2-ld-credex-issuer-proposal/02-counter-offer-sent.json");
        String requestReceived = loader.load("files/v2-ld-credex-issuer-proposal/03-request-received.json");
        String credentialIssued = loader.load("files/v2-ld-credex-issuer-proposal/04-credential-issued.json");

        V20CredExRecord proposal = ep.parseValueSave(proposalReceived, V20CredExRecord.class).orElseThrow();
        V20CredExRecord offer = ep.parseValueSave(counterOffer, V20CredExRecord.class).orElseThrow();
        V20CredExRecord request = ep.parseValueSave(requestReceived, V20CredExRecord.class).orElseThrow();
        V20CredExRecord issued = ep.parseValueSave(credentialIssued, V20CredExRecord.class).orElseThrow();

        Mockito.when(ac.walletDidPublic()).thenReturn(Optional.of(DID.builder()
                .did("did:indy:1234").method(DID.MethodEnum.SOV).build()));
        Mockito.when(
                ac.issueCredentialV2RecordsSendOffer(Mockito.anyString(), Mockito.any(V20CredBoundOfferRequest.class)))
                .thenReturn(Optional.of(offer));
        String id = proposal.getCredentialExchangeId();

        createDefaultPartner(proposal.getConnectionId());
        createDefaultSchema();

        aeh.handleCredentialV2(proposal);
        BPACredentialExchange ex = loadCredEx(id);
        Assertions.assertTrue(ex.stateIsProposalReceived());
        Assertions.assertTrue(ex.typeIsJsonLd());
        Assertions.assertEquals(ExchangeVersion.V2, ex.getExchangeVersion());
        Assertions.assertEquals(2, ex.proposalAttributesToMap().size());
        Assertions.assertEquals("Name", ex.proposalAttributesToMap().get("name"));
        Assertions.assertEquals(0, ex.offerAttributesToMap().size());

        CredentialOfferRequest req = new CredentialOfferRequest();
        req.setAcceptProposal(Boolean.TRUE);
        req.setSchemaId(schemaId);
        issuer.sendCredentialOffer(ex.getId(), req);

        aeh.handleCredentialV2(offer);
        ex = loadCredEx(id);
        Assertions.assertTrue(ex.stateIsOfferSent());

        aeh.handleCredentialV2(request);
        aeh.handleCredentialV2(issued);

        ex = loadCredEx(id);
        Assertions.assertTrue(ex.stateIsCredentialIssued());
        Assertions.assertEquals(2, ex.credentialAttributesToMap().size());
        Assertions.assertEquals("Other Name", ex.credentialAttributesToMap().get("name"));
    }

    @Test
    void testHandleIssuerProposalReceivedIssuerDeclines() {
        String proposalReceived = loader.load("files/v2-ld-credex-issuer-proposal/01-proposal-received.json");
        V20CredExRecord proposal = ep.parseValueSave(proposalReceived, V20CredExRecord.class).orElseThrow();

        createDefaultPartner(proposal.getConnectionId());
        createDefaultSchema();

        aeh.handleCredentialV2(proposal);

        BPACredentialExchange ex = loadCredEx(proposal.getCredentialExchangeId());
        issuer.declineCredentialProposal(ex.getId(), "my reason");

        ex = loadCredEx(proposal.getCredentialExchangeId());
        Assertions.assertTrue(ex.stateIsDeclined());
        Assertions.assertEquals("my reason", ex.getErrorMsg());
    }

    private BPACredentialExchange loadCredEx(String id) {
        return credExRepo.findByCredentialExchangeId(id)
                .orElseThrow();
    }

    private SchemaAPI createDefaultSchema() {
        return schemaService.addJsonLDSchema(schemaId, "Citizen",
                null, "PermanentResident", Set.of("name", "identifier"));
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
