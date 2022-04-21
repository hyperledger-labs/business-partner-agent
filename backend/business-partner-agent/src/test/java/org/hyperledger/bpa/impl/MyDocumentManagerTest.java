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
package org.hyperledger.bpa.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import io.micronaut.context.env.Environment;
import io.micronaut.data.model.Pageable;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.RunWithAries;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.MyDocumentAPI;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.client.CachingAriesClient;
import org.hyperledger.bpa.impl.activity.DocumentValidator;
import org.hyperledger.bpa.impl.aries.credential.CredentialTestUtils;
import org.hyperledger.bpa.impl.aries.jsonld.SignVerifyLD;
import org.hyperledger.bpa.impl.aries.jsonld.VPManager;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.impl.aries.wallet.Identity;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.repository.DidDocWebRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@MicronautTest(environments = { Environment.TEST, "test-web" })
class MyDocumentManagerTest extends RunWithAries {

    private static final Gson PRETTY = GsonConfig.prettyPrinter();

    @Inject
    MyDocumentManager mgmt;

    @Inject
    SignVerifyLD cryptoMgmt;

    @Inject
    Identity id;

    @Inject
    CachingAriesClient cAC;

    @Inject
    VPManager vpMgmt;

    @Inject
    DidDocWebRepository didDocRepo;

    @Inject
    DocumentValidator validator;

    private CredentialTestUtils utils;

    @BeforeEach
    public void setupUtils() {
        utils = new CredentialTestUtils(mapper);
    }

    @BeforeEach
    public void setupAcaPy() {
        cryptoMgmt.setAcaPy(ac);
        id.setAcaPy(ac);
        cAC.setAc(ac);

        SchemaService s = Mockito.mock(SchemaService.class);
        Mockito.when(s.getSchemaFor(Mockito.anyString())).thenReturn(Optional.of(BPASchema
                .builder()
                .label("dummy")
                .schemaAttributeNames(Set.of("iban"))
                .build()));
        validator.setSchemaService(s);
        mgmt.setValidator(validator);
    }

    @AfterEach
    public void tearDown() {
        didDocRepo.deleteAll();
    }

    @Test
    void testSaveNewCredential() throws Exception {
        MyDocumentAPI myCred = createAndSaveDummyCredential();
        assertNotNull(myCred);
        assertNotNull(myCred.getId());
        assertSame(CredentialType.INDY, myCred.getType());
        assertFalse(myCred.getIsPublic());
        assertTrue(Instant.now().isAfter(Instant.ofEpochMilli(myCred.getCreatedAt())));
        assertTrue(Instant.now().minus(5L, ChronoUnit.MINUTES).isBefore(
                Instant.ofEpochMilli(myCred.getCreatedAt())));
    }

    @Test
    void testSaveMasterdataCredTwiceNotPossible() {
        Exception exception = assertThrows(WrongApiUsageException.class, () -> {
            createAndSaveDummyCredential(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL, Boolean.FALSE);
            createAndSaveDummyCredential(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL, Boolean.TRUE);
        });

        String expectedMessage = "Organizational profile already exists";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testSaveIndyDocumentNoSchemaId() {
        Exception exception = assertThrows(WrongApiUsageException.class, () -> {
            MyDocumentAPI document = new MyDocumentAPI();
            document.setType(CredentialType.INDY);
            mgmt.saveNewDocument(document);
        });

        String expectedMessage = "A document of type indy_credential must have a schema id set";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testUpdateCredential() throws Exception {
        // create, save and verify
        MyDocumentAPI credential = utils.createDummyCred(CredentialType.INDY, Boolean.FALSE);
        MyDocumentAPI myCred = mgmt.saveNewDocument(credential);

        Optional<MyDocumentAPI> result = mgmt.getMyDocumentById(myCred.getId());
        assertTrue(result.isPresent());
        assertSame(CredentialType.INDY, result.get().getType());
        assertSame(Boolean.FALSE, result.get().getIsPublic());

        // update
        result.get().setIsPublic(Boolean.TRUE);
        mgmt.updateDocument(result.get().getId(), result.get());

        // verify modification
        result = mgmt.getMyDocumentById(myCred.getId());
        assertTrue(result.isPresent());
        assertSame(CredentialType.INDY, result.get().getType());
        assertSame(Boolean.TRUE, result.get().getIsPublic());
    }

    @Test
    void testGetMyCredentialById() throws Exception {
        MyDocumentAPI credential = utils.createDummyCred(CredentialType.INDY, Boolean.FALSE);
        MyDocumentAPI myCred = mgmt.saveNewDocument(credential);
        UUID ID = myCred.getId();

        assertEquals(0, mgmt.getMyDocuments(Pageable.unpaged(),
                CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL).getTotalSize());
        assertEquals(1, mgmt.getMyDocuments(Pageable.unpaged(), CredentialType.INDY).getTotalSize());
        Optional<MyDocumentAPI> result = mgmt.getMyDocumentById(ID);
        assertTrue(result.isPresent());

        result = mgmt.getMyDocumentById(UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetMyCredentials() throws Exception {
        assertEquals(0, mgmt.getMyDocuments(Pageable.unpaged()).getTotalSize());
        createAndSaveDummyCredential();
        assertEquals(1, mgmt.getMyDocuments(Pageable.unpaged()).getTotalSize());
        createAndSaveDummyCredential();
        assertEquals(2, mgmt.getMyDocuments(Pageable.unpaged()).getTotalSize());
    }

    @Test
    void testDeleteMyCredential() throws Exception {
        MyDocumentAPI credential = utils.createDummyCred(CredentialType.INDY, Boolean.FALSE);
        MyDocumentAPI myCred = mgmt.saveNewDocument(credential);
        UUID ID = myCred.getId();

        MyDocumentAPI credential2 = utils.createDummyCred(CredentialType.INDY, Boolean.FALSE);
        MyDocumentAPI myCred2 = mgmt.saveNewDocument(credential2);
        UUID ID2 = myCred2.getId();

        assertEquals(2, mgmt.getMyDocuments(Pageable.unpaged()).getTotalSize());

        mgmt.deleteMyDocumentById(ID);

        assertEquals(1, mgmt.getMyDocuments(Pageable.unpaged()).getTotalSize());
        assertEquals(ID2, mgmt.getMyDocuments(Pageable.unpaged()).getContent().get(0).getId());
    }

    @Test
    void testRecreateVPDefault() throws Exception {
        vpMgmt.recreateVerifiablePresentation();
        waitForVP(vpMgmt, false);
        final Optional<VerifiablePresentation<VerifiableIndyCredential>> vp = vpMgmt.getVerifiablePresentation();
        assertTrue(vp.isPresent());
        assertTrue(vp.get().getVerifiableCredential().isEmpty());
        assertNotNull(vp.get().getProof());
        log.debug(PRETTY.toJson(vp.get()));
    }

    @Test
    void testRecreateVPDefaultAddRemoveCredential() throws Exception {
        Optional<VerifiablePresentation<VerifiableIndyCredential>> vp = vpMgmt.getVerifiablePresentation();
        assertFalse(vp.isPresent());

        final MyDocumentAPI vc = createAndSavePublicDummyCredential();
        waitForVP(vpMgmt, false);

        vp = vpMgmt.getVerifiablePresentation();
        assertTrue(vp.isPresent());
        assertNotNull(vp.get().getVerifiableCredential());
        assertEquals(1, vp.get().getVerifiableCredential().size());
        log.debug(PRETTY.toJson(vp.get()));

        vc.setIsPublic(Boolean.FALSE);
        mgmt.updateDocument(vc.getId(), vc);

        waitForVCDeletion(vpMgmt);
        vp = vpMgmt.getVerifiablePresentation();
        assertTrue(vp.isPresent());
        assertTrue(vp.get().getVerifiableCredential().isEmpty());
    }

    private MyDocumentAPI createAndSaveDummyCredential() throws JsonProcessingException {
        return createAndSaveDummyCredential(CredentialType.INDY, Boolean.FALSE);
    }

    private MyDocumentAPI createAndSavePublicDummyCredential() throws JsonProcessingException {
        return createAndSaveDummyCredential(CredentialType.INDY, Boolean.TRUE);
    }

    private MyDocumentAPI createAndSaveDummyCredential(CredentialType credType, Boolean isPublic)
            throws JsonProcessingException {
        return mgmt.saveNewDocument(utils.createDummyCred(credType, isPublic));
    }
}
