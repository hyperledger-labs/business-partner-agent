package org.hyperledger.bpa.impl.aries;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.checkerframework.checker.optional.qual.Present;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.present_proof.*;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationRequest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;

import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.*;
import io.micronaut.core.type.Argument;

public class ProofManagerTest {

    @Mock
    private AriesClient aries;

    @InjectMocks
    private ProofManager proofManager;

    private PresentationExchangeRecord presentationExchangeRecord = new PresentationExchangeRecord();

    private List<PresentationRequestCredentials> validWalletCredentials = new ArrayList<>();

    @Test
    void testProofConstructionOneReqAttr() {
        // Create cred

        // create request for some attirbutes

        // mock /present-proof/<pres_exch_id>/credentials GET

        // Setup capture of ProofPresentation

        // call method

        // assert actual parameter to ac.presentProofRecordsSendPresentation
    }

    @Test
    void testProofConstructionTwoReqAttrfromDiffCreds() {
    }

    @Test
    void testProofConstructionOneReqAttrWithSchemaRestriction() {
    }

    @Test
    void testProofConstructionOnePredicate() {
    }

    @Test
    void testProofConstructionOnePredicatewithSchemaRestriction() {
    }

    @Test
    void testProofConstructionOneReqAttrOnePredicateWithSchemaRestrictions() {
    }

}
