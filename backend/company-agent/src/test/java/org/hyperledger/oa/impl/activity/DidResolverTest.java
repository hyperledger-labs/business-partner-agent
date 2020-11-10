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
package org.hyperledger.oa.impl.activity;

import org.hyperledger.oa.BaseTest;
import org.hyperledger.oa.api.DidDocAPI;
import org.hyperledger.oa.api.PartnerAPI;
import org.hyperledger.oa.client.URClient;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.Partner;
import org.hyperledger.oa.model.PartnerProof;
import org.hyperledger.oa.repository.PartnerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DidResolverTest extends BaseTest {

    private final String baSchemaId = "M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0";
    private final String crSchemaId = "8faozNpSjFfPJXYtgcPtmJ:2:commercialregister:1.2";

    @Mock
    PartnerRepository partnerRepo;

    @Mock
    PartnerLookup partnerLookup;

    @Mock
    URClient ur;

    @Mock
    Converter converter;

    @InjectMocks
    DidResolver didResolver;

    @Test
    void testIgnoreWrongSchema() {
        PartnerProof pp = PartnerProof.builder().schemaId(baSchemaId).build();
        didResolver.resolveDid(pp);
        verify(partnerRepo, never()).findById(any());
    }

    @Test
    void testIgnoreOutgoingConnection() {
        PartnerProof pp = PartnerProof.builder().schemaId(crSchemaId).build();
        when(partnerRepo.findById(any())).thenReturn(Optional.of(Partner.builder().incoming(Boolean.FALSE).build()));
        didResolver.resolveDid(pp);
        verify(ur, never()).getDidDocument(any());
    }

    @Test
    void testIgnoreIncomingConnectionWithPublicDid() {
        PartnerProof pp = PartnerProof.builder().schemaId(crSchemaId).build();
        when(partnerRepo.findById(any())).thenReturn(Optional.of(Partner.builder().incoming(Boolean.TRUE).build()));
        when(ur.getDidDocument(any())).thenReturn(Optional.of(new DidDocAPI()));
        didResolver.resolveDid(pp);
        verify(partnerLookup, never()).lookupPartner(any());
    }

    @Test
    void testIgnoreMissingDid() {
        PartnerProof pp = PartnerProof.builder()
                .schemaId(crSchemaId)
                .proof(Map.of("other", "not-a-did"))
                .build();
        when(partnerRepo.findById(any())).thenReturn(Optional.of(Partner.builder().incoming(Boolean.TRUE).build()));
        when(ur.getDidDocument(any())).thenReturn(Optional.of(new DidDocAPI()));
        didResolver.resolveDid(pp);
        verify(partnerLookup, never()).lookupPartner(any());
    }

    @Test
    void testResolveDidAndUpdatePartner() {
        PartnerProof pp = PartnerProof.builder()
                .schemaId(crSchemaId)
                .proof(Map.of("did", "did:dummy"))
                .build();
        when(partnerRepo.findById(any())).thenReturn(Optional.of(Partner.builder().incoming(Boolean.TRUE).build()));
        when(ur.getDidDocument(any())).thenReturn(Optional.empty());
        when(partnerLookup.lookupPartner(any())).thenReturn(new PartnerAPI());
        didResolver.resolveDid(pp);

        verify(partnerRepo, times(1)).findById(any());
        verify(partnerRepo, times(1)).update(any());
        verify(ur, times(1)).getDidDocument(any());
        verify(partnerLookup, times(1)).lookupPartner(any());
    }

}
