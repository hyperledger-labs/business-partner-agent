/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/organizational-agent

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.hyperledger.oa.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.oa.RunWithAries;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.api.MyDocumentAPI;
import org.hyperledger.oa.api.exception.WrongApiUsageException;
import org.hyperledger.oa.client.CachingAriesClient;
import org.hyperledger.oa.impl.activity.CryptoManager;
import org.hyperledger.oa.impl.activity.Identity;
import org.hyperledger.oa.impl.activity.VPManager;
import org.hyperledger.oa.repository.DidDocWebRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@MicronautTest(environments = { Environment.TEST, "test-aries" })
class MyDocumentManagerTest extends RunWithAries {

    private static final Gson PRETTY = GsonConfig.prettyPrinter();

    @Inject
    MyDocumentManager mgmt;

    @Inject
    CryptoManager cryptoMgmt;

    @Inject
    Identity id;

    @Inject
    CachingAriesClient cAC;

    @Inject
    VPManager vpMgmt;

    @Inject
    DidDocWebRepository didDocRepo;

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
        assertSame(CredentialType.BANK_ACCOUNT_CREDENTIAL, myCred.getType());
        assertFalse(myCred.getIsPublic());
        assertTrue(Instant.now().isAfter(Instant.ofEpochMilli(myCred.getCreatedDate())));
        assertTrue(Instant.now().minus(5L, ChronoUnit.MINUTES).isBefore(
                Instant.ofEpochMilli(myCred.getCreatedDate())));
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
    void testUpdateCredential() throws Exception {
        // create, save and verify
        MyDocumentAPI credential = utils.createDummyCred(CredentialType.BANK_ACCOUNT_CREDENTIAL, Boolean.FALSE);
        MyDocumentAPI myCred = mgmt.saveNewDocument(credential);

        Optional<MyDocumentAPI> result = mgmt.getMyDocumentById(myCred.getId());
        assertTrue(result.isPresent());
        assertSame(CredentialType.BANK_ACCOUNT_CREDENTIAL, result.get().getType());
        assertSame(Boolean.FALSE, result.get().getIsPublic());

        // update
        result.get().setIsPublic(Boolean.TRUE);
        mgmt.updateDocument(result.get().getId(), result.get());

        // verify modification
        result = mgmt.getMyDocumentById(myCred.getId());
        assertTrue(result.isPresent());
        assertSame(CredentialType.BANK_ACCOUNT_CREDENTIAL, result.get().getType());
        assertSame(Boolean.TRUE, result.get().getIsPublic());
    }

    @Test
    void testGetMyCredentialById() throws Exception {
        MyDocumentAPI credential = utils.createDummyCred(CredentialType.BANK_ACCOUNT_CREDENTIAL, Boolean.FALSE);
        MyDocumentAPI myCred = mgmt.saveNewDocument(credential);
        UUID ID = myCred.getId();

        assertEquals(1, mgmt.getMyDocuments().size());
        Optional<MyDocumentAPI> result = mgmt.getMyDocumentById(ID);
        assertTrue(result.isPresent());

        result = mgmt.getMyDocumentById(UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetMyCredentials() throws Exception {
        assertEquals(0, mgmt.getMyDocuments().size());
        createAndSaveDummyCredential();
        assertEquals(1, mgmt.getMyDocuments().size());
        createAndSaveDummyCredential();
        assertEquals(2, mgmt.getMyDocuments().size());
    }

    @Test
    void testDeleteMyCredential() throws Exception {
        MyDocumentAPI credential = utils.createDummyCred(CredentialType.BANK_ACCOUNT_CREDENTIAL, Boolean.FALSE);
        MyDocumentAPI myCred = mgmt.saveNewDocument(credential);
        UUID ID = myCred.getId();

        MyDocumentAPI credential2 = utils.createDummyCred(CredentialType.BANK_ACCOUNT_CREDENTIAL, Boolean.FALSE);
        MyDocumentAPI myCred2 = mgmt.saveNewDocument(credential2);
        UUID ID2 = myCred2.getId();

        assertEquals(2, mgmt.getMyDocuments().size());

        mgmt.deleteMyDocumentById(ID);

        assertEquals(1, mgmt.getMyDocuments().size());
        assertEquals(ID2, mgmt.getMyDocuments().get(0).getId());
    }

    @Test
    void testRecreateVPDefault() throws Exception {
        vpMgmt.recreateVerifiablePresentation();
        waitForVP(vpMgmt, false);
        final Optional<VerifiablePresentation<VerifiableIndyCredential>> vp = vpMgmt.getVerifiablePresentation();
        assertTrue(vp.isPresent());
        assertNull(vp.get().getVerifiableCredential());
        assertNotNull(vp.get().getProof());
        log.debug(PRETTY.toJson(vp));
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
        log.debug(PRETTY.toJson(vp));

        vc.setIsPublic(Boolean.FALSE);
        mgmt.updateDocument(vc.getId(), vc);

        waitForVCDeletion(vpMgmt);
        vp = vpMgmt.getVerifiablePresentation();
        assertNull(vp.get().getVerifiableCredential());
    }

    private MyDocumentAPI createAndSaveDummyCredential() throws JsonProcessingException {
        return createAndSaveDummyCredential(CredentialType.BANK_ACCOUNT_CREDENTIAL, Boolean.FALSE);
    }

    private MyDocumentAPI createAndSavePublicDummyCredential() throws JsonProcessingException {
        return createAndSaveDummyCredential(CredentialType.BANK_ACCOUNT_CREDENTIAL, Boolean.TRUE);
    }

    private MyDocumentAPI createAndSaveDummyCredential(CredentialType credType, Boolean isPublic)
            throws JsonProcessingException {
        return mgmt.saveNewDocument(utils.createDummyCred(credType, isPublic));
    }
}
