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

import io.micronaut.context.annotation.Value;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.model.BPARestrictions;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.BPARestrictionsRepository;
import org.hyperledger.bpa.repository.BPASchemaRepository;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@MicronautTest
public class PartnerCredDefLookupTest {

    @Value("${bpa.did.prefix}")
    String didPrefix;

    @Inject
    PartnerCredDefLookup lookup;

    @Inject
    BPASchemaRepository schemaRepo;

    @Inject
    BPARestrictionsRepository restrictionsRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Test
    void testFilterByConfiguredCredentialDefs() {
        String schemaId = "schema1";
        String did1 = didPrefix + "did1";
        String did2 = didPrefix + "did2";

        partnerRepo.save(Partner.builder().did(did1).ariesSupport(Boolean.TRUE).build());
        partnerRepo.save(Partner.builder().did(did2).ariesSupport(Boolean.TRUE).build());

        BPASchema dbSchema = schemaRepo.save(BPASchema.builder()
                .schemaId(schemaId).seqNo(1).label("dummy").schemaAttributeNames(Set.of("name")).build());
        restrictionsRepo.save(BPARestrictions.builder().schema(dbSchema).issuerDid(did1)
                .build());
        restrictionsRepo.save(BPARestrictions.builder().schema(dbSchema).issuerDid(did2)
                .build());
        restrictionsRepo.save(BPARestrictions.builder().schema(dbSchema).issuerDid("did3")
                .build());

        List<PartnerAPI> result = new ArrayList<>();
        lookup.filterByConfiguredCredentialDefs(schemaId, result);

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(did1, result.get(0).getDid());
        Assertions.assertEquals(did2, result.get(1).getDid());
    }

    @Test
    void testNoCredDefConfigured() {
        String schemaId = "schema_1";
        schemaRepo.save(BPASchema.builder()
                .schemaId(schemaId).seqNo(1).label("dummy").schemaAttributeNames(Set.of("name")).build());

        Optional<BPASchema> dbSchema = schemaRepo.findBySchemaId(schemaId);
        Assertions.assertTrue(dbSchema.isPresent());

        List<PartnerAPI> result = new ArrayList<>();
        lookup.filterByConfiguredCredentialDefs(schemaId, result);
        Assertions.assertEquals(0, result.size());
    }
}
