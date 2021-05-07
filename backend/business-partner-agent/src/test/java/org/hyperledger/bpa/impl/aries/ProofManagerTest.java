package org.hyperledger.bpa.impl.aries;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.checkerframework.checker.optional.qual.Present;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationRequest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;

import org.mockito.Mock;
import org.mockito.Mockito;

import io.micronaut.core.type.Argument;



public class ProofManagerTest {
    
    @Mock
    private AriesClient aries;

    @InjectMocks
    private ProofManager proofManager;

    
    private PresentationExchangeRecord presentationExchangeRecord = new PresentationExchangeRecord();
    
    // private List<PresentationRequestCredentials> presentationRequestCreds = new ArrayList<>();


    @Test
    void testProofConstructionOneReqAttr() 
    {}


    @Test
    void testProofConstructionTwoReqAttrfromDiffCreds() {}

    @Test
    void testProofConstructionOneReqAttrWithSchemaRestriction() {}

    @Test
    void testProofConstructionOnePredicate() {}

    @Test
    void testProofConstructionOnePredicatewithSchemaRestriction() {}

    @Test
    void testProofConstructionOneReqAttrOnePredicateWithSchemaRestrictions() {}


}
