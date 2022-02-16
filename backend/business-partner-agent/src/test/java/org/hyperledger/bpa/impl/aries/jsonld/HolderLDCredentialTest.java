package org.hyperledger.bpa.impl.aries.jsonld;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.NonNull;
import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V2IssueLDCredentialEvent;
import org.hyperledger.aries.webhook.EventParser;
import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.impl.aries.AriesEventHandler;
import org.hyperledger.bpa.impl.aries.credential.HolderManager;
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
import java.util.Optional;

@MicronautTest
@ExtendWith(MockitoExtension.class)
public class HolderLDCredentialTest extends BaseTest {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    HolderCredExRepository credExRepo;

    @Inject
    HolderManager holder;

    @Inject
    AriesEventHandler aeh;

    @Inject
    AriesClient ac;

    private final EventParser ep = new EventParser();

    @Test
    void testHolderReceivesCredentialFromIssuerAndAccepts() throws IOException {
        Mockito.when(ac.walletDidCreate(Mockito.any()))
                .thenReturn(Optional.of(DID.builder().did("did:key:dummy").build()));

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
        Assertions.assertEquals(2, ex.offerAttributesToMap().size());
        Assertions.assertEquals("karl", ex.offerAttributesToMap().get("name"));

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
        Assertions.assertEquals(2, ex.credentialAttributesToMap().size());
        Assertions.assertEquals("karl", ex.credentialAttributesToMap().get("name"));
    }

    @Test
    void testHolderReceivesCredentialFromIssuerAndDeclines() {

    }

    @Test
    void testHolderRequestsCredentialFromIssuerAndIssuerAccepts() {

    }

    @Test
    void testHolderRequestsCredentialFromIssuerAndIssuerDeclines() {

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
}
