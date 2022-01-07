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

import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.NonNull;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.RunWithAries;
import org.hyperledger.bpa.client.CachingAriesClient;
import org.hyperledger.bpa.impl.activity.CryptoManager;
import org.hyperledger.bpa.impl.activity.Identity;
import org.hyperledger.bpa.impl.activity.VPManager;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.model.BPACredentialExchange;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.DidDocWebRepository;
import org.hyperledger.bpa.repository.HolderCredExRepository;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(environments = { Environment.TEST, "test-web" })
@ExtendWith(MockitoExtension.class)
public class CredentialManagerIntegrationTest extends RunWithAries {

    @Inject
    AriesEventHandler eventHandler;

    @Inject
    HolderCredentialManager holderMgmt;

    @Inject
    HolderCredExRepository holderCredExRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    VPManager vpMgmt;

    @Inject
    CryptoManager crypto;

    @Inject
    Identity id;

    @Inject
    CachingAriesClient acaCache;

    @Inject
    DidDocWebRepository didDocRepo;

    @Mock
    SchemaService schemaService;

    @BeforeEach
    public void injectAries() {
        holderMgmt.setSchemaService(schemaService);
        crypto.setAcaPy(ac);
        acaCache.setAc(ac);
        id.setAcaPy(ac);
        id.setAcaCache(acaCache);
    }

    @AfterEach
    public void cleanUp() {
        didDocRepo.deleteAll();
    }

    @Test
    void testV1HolderFlowWithDelete() throws Exception {

        Mockito.when(schemaService.getSchemaFor(Mockito.anyString()))
                .thenReturn(Optional.of(BPASchema.builder().build()));

        // create credential
        final V1CredentialExchange offer = loadV1FileByStateName("01-offer");
        final V1CredentialExchange request = loadV1FileByStateName("02-request");
        final V1CredentialExchange received = loadV1FileByStateName("03-received");
        final V1CredentialExchange acked = loadV1FileByStateName("04-acked");
        createRandomPartner(offer.getConnectionId());

        eventHandler.handleCredential(offer);
        eventHandler.handleCredential(request);
        eventHandler.handleCredential(received);
        eventHandler.handleCredential(acked);

        assertEquals(1, holderCredExRepo.count());
        BPACredentialExchange ex = holderCredExRepo.findByCredentialExchangeId(offer.getCredentialExchangeId())
                .orElseThrow();
        assertEquals(CredentialExchangeState.CREDENTIAL_ACKED, ex.getState());
        assertNotNull(ex.getStateToTimestamp());
        assertNotNull(ex.getStateToTimestamp().getStateToTimestamp());
        assertEquals(4, ex.getStateToTimestamp().getStateToTimestamp().keySet().size());
        assertTrue(vpMgmt.getVerifiablePresentation().isEmpty());

        // make it public
        final UUID credId = holderCredExRepo.findAll().iterator().next().getId();
        holderMgmt.toggleVisibility(credId);
        waitForVP(vpMgmt, true);
        assertTrue(vpMgmt.getVerifiablePresentation().isPresent());
        assertNotNull(vpMgmt.getVerifiablePresentation().get().getVerifiableCredential());
        assertEquals(1, vpMgmt.getVerifiablePresentation().get().getVerifiableCredential().size());

        // delete it again
        holderMgmt.deleteCredentialById(credId);
        waitForVCDeletion(vpMgmt);
        assertTrue(vpMgmt.getVerifiablePresentation().isPresent());
        assertNotNull(vpMgmt.getVerifiablePresentation().get().getVerifiableCredential());
        assertTrue(vpMgmt.getVerifiablePresentation().get().getVerifiableCredential().isEmpty());
    }

    @Test
    void testV1DeclineCredentialOffer() {

        Mockito.when(schemaService.getSchemaFor(Mockito.anyString()))
                .thenReturn(Optional.of(BPASchema.builder().build()));

        // create credential
        final V1CredentialExchange offer = loadV1FileByStateName("01-offer");
        final V1CredentialExchange problem = loadV1FileByStateName("05-problem");
        createRandomPartner(offer.getConnectionId());

        eventHandler.handleCredential(offer);

        BPACredentialExchange ex = holderCredExRepo.findByCredentialExchangeId(offer.getCredentialExchangeId())
                .orElseThrow();
        assertEquals(CredentialExchangeState.OFFER_RECEIVED, ex.getState());

        holderMgmt.declineCredentialOffer(ex.getId(), "declined");
        eventHandler.handleCredential(problem);

        ex = holderCredExRepo.findByCredentialExchangeId(offer.getCredentialExchangeId()).orElseThrow();
        assertEquals(CredentialExchangeState.DECLINED, ex.getState());
    }

    @Test
    void testV2HolderFlowWithDelete() throws Exception {

        Mockito.when(schemaService.getSchemaFor(Mockito.anyString()))
                .thenReturn(Optional.of(BPASchema.builder().build()));

        // create credential
        final V20CredExRecord offer = loadV2FileByStateName("01-offer");
        final V20CredExRecord request = loadV2FileByStateName("02-request");
        final V20CredExRecord received = loadV2FileByStateName("03-received");
        final V20CredExRecord done = loadV2FileByStateName("05-done");
        createRandomPartner(offer.getConnectionId());

        eventHandler.handleCredentialV2(offer);
        eventHandler.handleCredentialV2(request);
        eventHandler.handleCredentialV2(received);
        eventHandler.handleCredentialV2(done);

        BPACredentialExchange ex = holderCredExRepo.findByCredentialExchangeId(offer.getCredentialExchangeId())
                .orElseThrow();
        assertEquals(CredentialExchangeState.DONE, ex.getState());
        assertNotNull(ex.getStateToTimestamp());
        assertNotNull(ex.getStateToTimestamp().getStateToTimestamp());
        assertEquals(4, ex.getStateToTimestamp().getStateToTimestamp().keySet().size());
        assertTrue(vpMgmt.getVerifiablePresentation().isEmpty());

        // make it public
        final UUID credId = holderCredExRepo.findAll().iterator().next().getId();
        holderMgmt.toggleVisibility(credId);
        waitForVP(vpMgmt, true);
        assertTrue(vpMgmt.getVerifiablePresentation().isPresent());
        assertNotNull(vpMgmt.getVerifiablePresentation().get().getVerifiableCredential());
        assertEquals(1, vpMgmt.getVerifiablePresentation().get().getVerifiableCredential().size());

        // delete it again
        holderMgmt.deleteCredentialById(credId);
        waitForVCDeletion(vpMgmt);
        assertTrue(vpMgmt.getVerifiablePresentation().isPresent());
        assertNotNull(vpMgmt.getVerifiablePresentation().get().getVerifiableCredential());
        assertTrue(vpMgmt.getVerifiablePresentation().get().getVerifiableCredential().isEmpty());
    }

    @Test
    void testV2DeclineCredentialOffer() {

        Mockito.when(schemaService.getSchemaFor(Mockito.anyString()))
                .thenReturn(Optional.of(BPASchema.builder().build()));

        final V20CredExRecord offer = loadV2FileByStateName("01-offer");
        final V20CredExRecord problem = loadV2FileByStateName("06-problem");
        createRandomPartner(offer.getConnectionId());

        eventHandler.handleCredentialV2(offer);

        BPACredentialExchange ex = holderCredExRepo.findByCredentialExchangeId(offer.getCredentialExchangeId())
                .orElseThrow();
        assertEquals(CredentialExchangeState.OFFER_RECEIVED, ex.getState());

        holderMgmt.declineCredentialOffer(ex.getId(), "declined");
        eventHandler.handleCredentialV2(problem);

        ex = holderCredExRepo.findByCredentialExchangeId(offer.getCredentialExchangeId()).orElseThrow();
        assertEquals(CredentialExchangeState.DECLINED, ex.getState());
    }

    private V1CredentialExchange loadV1FileByStateName(@NonNull String state) {
        final String ex = loader.load("files/v1-credex-holder/" + state + ".json");
        return GsonConfig.defaultConfig().fromJson(ex, V1CredentialExchange.class);
    }

    private V20CredExRecord loadV2FileByStateName(@NonNull String state) {
        final String ex = loader.load("files/v2-credex-holder/" + state + ".json");
        return GsonConfig.defaultConfig().fromJson(ex, V20CredExRecord.class);
    }

    private void createRandomPartner(String connectionId) {
        partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .did(UUID.randomUUID().toString())
                .connectionId(connectionId)
                .state(ConnectionState.ACTIVE)
                .trustPing(Boolean.TRUE)
                .build());
    }
}
