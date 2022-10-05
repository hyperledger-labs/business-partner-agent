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
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V2CredentialExchangeFree;
import org.hyperledger.aries.api.issue_credential_v2.V2IssueLDCredentialEvent;
import org.hyperledger.aries.webhook.EventParser;
import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.MyDocumentAPI;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.impl.MyDocumentManager;
import org.hyperledger.bpa.impl.aries.AriesEventHandler;
import org.hyperledger.bpa.impl.aries.credential.HolderManager;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.repository.HolderCredExRepository;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

@MicronautTest
@ExtendWith(MockitoExtension.class)
public class HolderLDCredentialTest extends BaseTest {

    private final String schemaId = "https://w3id.org/citizenship/v1";

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    HolderCredExRepository credExRepo;

    @Inject
    MyDocumentManager doc;

    @Inject
    SchemaService schemaService;

    @Inject
    HolderManager holder;

    @Inject
    Converter conv;

    @Inject
    AriesEventHandler aeh;

    @Inject
    AriesClient ac;

    private final EventParser ep = new EventParser();

    @Test
    void testHolderReceivesCredentialFromIssuerAndAccepts() throws IOException {
        Mockito.when(ac.walletDidPublic()).thenReturn(Optional.of(DID.builder()
                .did("did:indy:1234").method(DID.MethodEnum.SOV).build()));

        String offerReceived = loader.load("files/v2-ld-credex-holder/01-offer-received.json");
        String requestSent = loader.load("files/v2-ld-credex-holder/02-request-sent.json");
        String credentialReceived = loader.load("files/v2-ld-credex-holder/03-credential-received.json");
        String ldProofIds = loader.load("files/v2-ld-credex-holder/04-issue-credential-ld-proof.json");
        String credDone = loader.load("files/v2-ld-credex-holder/05-done.json");

        V20CredExRecord offer = ep.parseValueSave(offerReceived, V20CredExRecord.class).orElseThrow();
        V20CredExRecord request = ep.parseValueSave(requestSent, V20CredExRecord.class).orElseThrow();
        V20CredExRecord received = ep.parseValueSave(credentialReceived, V20CredExRecord.class).orElseThrow();
        V2IssueLDCredentialEvent ldIds = ep.parseValueSave(ldProofIds, V2IssueLDCredentialEvent.class).orElseThrow();
        V20CredExRecord done = ep.parseValueSave(credDone, V20CredExRecord.class).orElseThrow();

        String id = offer.getCredentialExchangeId();

        createDefaultPartner(offer.getConnectionId());

        aeh.handleCredentialV2(offer);
        BPACredentialExchange ex = loadCredEx(id);
        Assertions.assertTrue(ex.stateIsOfferReceived());
        Assertions.assertTrue(ex.typeIsJsonLd());
        Assertions.assertEquals(ExchangeVersion.V2, ex.getExchangeVersion());
        Assertions.assertEquals(2, ex.offerAttributesToCredentialAttributesList().size());
        Assertions.assertEquals("karl", ex.credentialAttributesToCredentialAttributesList().stream()
                .filter(attr -> attr.getName().equals("name")).findFirst().get().getValue());

        holder.sendCredentialRequest(ex.getId());

        aeh.handleCredentialV2(request);
        ex = loadCredEx(id);
        Assertions.assertTrue(ex.stateIsRequestSent());

        aeh.handleCredentialV2(received);
        ex = loadCredEx(id);
        Assertions.assertTrue(ex.stateIsCredentialReceived());

        aeh.handleIssueCredentialV2LD(ldIds);
        ex = loadCredEx(id);
        Assertions.assertEquals("2d9afcfd4a2145bcb5253da9890200e0", ex.getReferent());

        aeh.handleCredentialV2(done);
        ex = loadCredEx(id);
        Assertions.assertTrue(ex.stateIsDone());
        Assertions.assertEquals(2, ex.credentialAttributesToCredentialAttributesList().size());
        Assertions.assertEquals("karl", ex.credentialAttributesToCredentialAttributesList().stream()
                .filter(attr -> attr.getName().equals("name")).findFirst().get().getValue());
    }

    @Test
    void testHolderReceivesCredentialFromIssuerAndDeclines() {
        String offerReceived = loader.load("files/v2-ld-credex-holder/01-offer-received.json");

        V20CredExRecord offer = ep.parseValueSave(offerReceived, V20CredExRecord.class).orElseThrow();

        String id = offer.getCredentialExchangeId();

        createDefaultPartner(offer.getConnectionId());

        aeh.handleCredentialV2(offer);
        BPACredentialExchange ex = loadCredEx(id);
        Assertions.assertTrue(ex.stateIsOfferReceived());

        holder.declineCredentialOffer(ex.getId(), "my reason");
        ex = loadCredEx(id);
        Assertions.assertTrue(ex.stateIsDeclined());
        Assertions.assertEquals("my reason", ex.getErrorMsg());
    }

    @Test
    void testHolderRequestsCredentialFromIssuerAndIssuerAccepts() throws IOException {
        String proposalSent = loader.load("files/v2-ld-credex-holder-proposal/01-proposal-sent.json");
        String offerReceived = loader.load("files/v2-ld-credex-holder-proposal/02-offer-received.json");

        V20CredExRecord proposal = ep.parseValueSave(proposalSent, V20CredExRecord.class).orElseThrow();
        V20CredExRecord offer = ep.parseValueSave(offerReceived, V20CredExRecord.class).orElseThrow();

        Mockito.when(ac.walletDidPublic()).thenReturn(Optional.of(DID.builder()
                .did("did:indy:1234").method(DID.MethodEnum.SOV).build()));
        Mockito.when(
                ac.issueCredentialV2SendProposal(Mockito.any(V2CredentialExchangeFree.class)))
                .thenReturn(Optional.of(proposal));

        String id = offer.getCredentialExchangeId();

        Partner p = createDefaultPartner(offer.getConnectionId());
        createDefaultSchema();

        MyDocumentAPI document = doc.saveNewDocument(MyDocumentAPI.builder()
                .schemaId(schemaId)
                .type(CredentialType.JSON_LD)
                .isPublic(Boolean.FALSE)
                .documentData(new ArrayList<>(Arrays.asList(
                        new CredentialAttributes("name", "My Name", null),
                        new CredentialAttributes("identifier", "something", null))))
                .build());

        holder.sendCredentialProposal(p.getId(), document.getId(), null);

        aeh.handleCredentialV2(proposal);
        BPACredentialExchange ex = loadCredEx(id);
        Assertions.assertTrue(ex.stateIsProposalSent());
        Assertions.assertTrue(ex.typeIsJsonLd());
        Assertions.assertEquals(ExchangeVersion.V2, ex.getExchangeVersion());
        Assertions.assertEquals(2, ex.proposalAttributesToCredentialAttributesList().size());
        Assertions.assertEquals("My Name", ex.credentialAttributesToCredentialAttributesList().stream()
                .filter(attr -> attr.getName().equals("name")).findFirst().get().getValue());

        aeh.handleCredentialV2(offer);
        ex = loadCredEx(id);
        Assertions.assertTrue(ex.stateIsOfferReceived());

        // from here on same as testHolderReceivesCredentialFromIssuerAndAccepts()
    }

    @Test
    void testHolderRequestsCredentialFromIssuerAndIssuerDeclines() throws IOException {
        String proposalSent = loader.load("files/v2-ld-credex-holder-proposal/01-proposal-sent.json");
        String abandonedEvent = loader.load("files/v2-ld-credex-holder-proposal/03-abandoned.json");

        V20CredExRecord proposal = ep.parseValueSave(proposalSent, V20CredExRecord.class).orElseThrow();
        V20CredExRecord abandoned = ep.parseValueSave(abandonedEvent, V20CredExRecord.class).orElseThrow();

        Mockito.when(ac.walletDidPublic()).thenReturn(Optional.of(DID.builder()
                .did("did:indy:1234").method(DID.MethodEnum.SOV).build()));
        Mockito.when(
                ac.issueCredentialV2SendProposal(Mockito.any(V2CredentialExchangeFree.class)))
                .thenReturn(Optional.of(proposal));

        String id = proposal.getCredentialExchangeId();

        Partner p = createDefaultPartner(proposal.getConnectionId());
        createDefaultSchema();

        MyDocumentAPI document = doc.saveNewDocument(MyDocumentAPI.builder()
                .schemaId(schemaId)
                .type(CredentialType.JSON_LD)
                .isPublic(Boolean.FALSE)
                .documentData(new ArrayList<>(Arrays.asList(
                        new CredentialAttributes("name", "My Name", null),
                        new CredentialAttributes("identifier", "something", null))))
                .build());

        holder.sendCredentialProposal(p.getId(), document.getId(), null);

        aeh.handleCredentialV2(proposal);
        BPACredentialExchange ex = loadCredEx(id);
        Assertions.assertTrue(ex.stateIsProposalSent());

        aeh.handleCredentialV2(abandoned);
        ex = loadCredEx(id);
        Assertions.assertTrue(ex.stateIsProblem());
        Assertions.assertEquals("issuance-abandoned: my reason", ex.getErrorMsg());
    }

    @Test
    void testFailWhenAddingSchemaTwice() {
        createDefaultSchema();
        Assertions.assertThrows(WrongApiUsageException.class, this::createDefaultSchema);
    }

    private BPACredentialExchange loadCredEx(String id) {
        return credExRepo.findByCredentialExchangeId(id)
                .orElseThrow();
    }

    private Partner createDefaultPartner(@NonNull String connectionId) {
        Partner p = Partner.builder()
                .connectionId(connectionId)
                .did("did:sov:dummy")
                .ariesSupport(Boolean.TRUE)
                .build();
        return partnerRepo.save(p);
    }

    private void createDefaultSchema() {
        schemaService.addJsonLDSchema(schemaId, "Citizen",
                null, "PermanentResident", Set.of("name", "identifier"));
    }
}
