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
package org.hyperledger.bpa.impl.activity;

import lombok.NonNull;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.resolver.DIDDocument;
import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.client.DidDocClient;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.PartnerProof;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DidResolverTest extends BaseTest {

    private final String crSchemaId = "8faozNpSjFfPJXYtgcPtmJ:2:commercialregister:1.2";

    @Mock
    PartnerRepository partnerRepo;

    @Mock
    PartnerLookup partnerLookup;

    @Mock
    DidDocClient ur;

    @Mock
    Converter converter;

    @InjectMocks
    DidResolver didResolver;

    @Test
    void testIgnoreWrongSchema() {
        PartnerProof pp = PartnerProof.builder().build();
        String baSchemaId = "M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0";
        didResolver.resolveDid(pp, buildIdentifiers(baSchemaId));
        verify(partnerRepo, never()).findById(any());
    }

    @Test
    void testIgnoreOutgoingConnection() {
        PartnerProof pp = PartnerProof.builder().build();
        when(partnerRepo.findById(any())).thenReturn(Optional.of(Partner.builder().incoming(Boolean.FALSE).build()));
        didResolver.resolveDid(pp, buildIdentifiers(crSchemaId));
        verify(ur, never()).getDidDocument(any());
    }

    @Test
    void testIgnoreIncomingConnectionWithPublicDid() {
        PartnerProof pp = PartnerProof.builder().build();
        when(partnerRepo.findById(any())).thenReturn(Optional.of(Partner.builder().incoming(Boolean.TRUE).build()));
        when(ur.getDidDocument(any())).thenReturn(Optional.of(new DIDDocument()));
        didResolver.resolveDid(pp, buildIdentifiers(crSchemaId));
        verify(partnerLookup, never()).lookupPartner(any());
    }

    @Test
    void testIgnoreMissingDid() {
        PartnerProof pp = PartnerProof.builder()
                .proof(Map.of("other", PresentationExchangeRecord.RevealedAttributeGroup
                        .builder()
                        .revealedAttribute("something", "not-a-did")
                        .build()))
                .build();
        when(partnerRepo.findById(any())).thenReturn(Optional.of(Partner.builder().incoming(Boolean.TRUE).build()));
        when(ur.getDidDocument(any())).thenReturn(Optional.of(new DIDDocument()));
        didResolver.resolveDid(pp, buildIdentifiers(crSchemaId));
        verify(partnerLookup, never()).lookupPartner(any());
    }

    @Test
    void testResolveDidAndUpdatePartner() {
        PartnerProof pp = PartnerProof.builder()
                .proof(Map.of("did", PresentationExchangeRecord.RevealedAttributeGroup
                        .builder()
                        .revealedAttribute("did", "did:dummy")
                        .build()))
                .build();
        when(partnerRepo.findById(any())).thenReturn(Optional.of(Partner.builder().incoming(Boolean.TRUE).build()));
        when(ur.getDidDocument(any())).thenReturn(Optional.empty());
        when(partnerLookup.lookupPartner(any())).thenReturn(new PartnerAPI());
        didResolver.resolveDid(pp, buildIdentifiers(crSchemaId));

        verify(partnerRepo, times(1)).findById(any());
        verify(partnerRepo, times(1)).update(any());
        verify(ur, times(1)).getDidDocument(any());
        verify(partnerLookup, times(1)).lookupPartner(any());
    }

    @Test
    void testSplitDid() {
        DidResolver.ConnectionLabel cl = DidResolver.splitDidFrom("did:sov:123:label");
        assertEquals("label", cl.getLabel());
        assertTrue(cl.getDid().isPresent());
        assertEquals("did:sov:123", cl.getDid().get());

        cl = DidResolver.splitDidFrom("did:sov:JTWwhv1L3ZBtX8WWBPJMRy:Bob's Agent");
        assertEquals("Bob's Agent", cl.getLabel());
        assertTrue(cl.getDid().isPresent());
        assertEquals("did:sov:JTWwhv1L3ZBtX8WWBPJMRy", cl.getDid().get());

        cl = DidResolver.splitDidFrom("did:label");
        assertEquals("did:label", cl.getLabel());
        assertTrue(cl.getDid().isEmpty());
    }

    private List<PresentationExchangeRecord.Identifier> buildIdentifiers(@NonNull String schemaId) {
        return List.of(PresentationExchangeRecord.Identifier.builder().schemaId(schemaId).build());
    }

}
