package org.hyperledger.oa.impl.aries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.hyperledger.oa.client.LedgerClient;
import org.hyperledger.oa.controller.api.partner.PartnerCredentialType;
import org.hyperledger.oa.model.BPASchema;
import org.hyperledger.oa.model.Partner;
import org.hyperledger.oa.repository.PartnerRepository;
import org.hyperledger.oa.repository.SchemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
@ExtendWith(MockitoExtension.class)
class PartnerCredDefLookupTest {

    @Mock
    LedgerClient ledger;

    @Mock
    SchemaRepository schemaRepo;

    @Inject
    PartnerCredDefLookup lookup;

    @Inject
    PartnerRepository pRepo;

    @BeforeEach
    public void setup() {
        lookup.setLedger(ledger);
        lookup.setSchemaRepo(schemaRepo);
        lookup.setDidPrefix("");
    }

    @Test
    void testLookup() {
        String did1 = "did-1";
        String did2 = "did-2";

        pRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .did(did1)
                .connectionId(did1)
                .build());

        pRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .did(did2)
                .connectionId(did2)
                .build());

        when(schemaRepo.findAll()).thenReturn(List.of(
                BPASchema.builder().seqNo(Integer.valueOf(1077)).build(),
                BPASchema.builder().seqNo(Integer.valueOf(977)).build(),
                BPASchema.builder().seqNo(Integer.valueOf(9999)).build()));

        when(ledger.queryCredentialDefinitions(anyString()))
                .thenReturn(Optional.of(List.of(
                        PartnerCredentialType.fromCredDefId("did-1:3:CL:1077:commercial register entry"),
                        PartnerCredentialType.fromCredDefId("other:3:CL:1077:commreg"))))
                .thenReturn(Optional.of(List.of(
                        PartnerCredentialType.fromCredDefId("did-1:3:CL:977:bank"),
                        PartnerCredentialType.fromCredDefId("did-2:3:CL:977:my-bank"))))
                .thenReturn(Optional.empty());

        lookup.lookupTypesForAllPartners();

        List<Partner> partners = pRepo.findBySuppertedCredential("1077");
        assertEquals(1, partners.size());
        assertEquals(did1, partners.get(0).getDid());

        partners = pRepo.findBySuppertedCredential("977");
        assertEquals(2, partners.size());
        assertEquals(did1, partners.get(0).getDid());
        assertEquals(did2, partners.get(1).getDid());

        partners = pRepo.findBySuppertedCredential("9999");
        assertEquals(0, partners.size());
    }

}
