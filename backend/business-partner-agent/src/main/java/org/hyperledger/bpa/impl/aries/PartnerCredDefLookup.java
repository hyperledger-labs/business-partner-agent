package org.hyperledger.bpa.impl.aries;

import io.micronaut.context.annotation.Value;
import io.micronaut.scheduling.annotation.Async;
import io.micronaut.scheduling.annotation.Scheduled;
import lombok.*;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.client.LedgerClient;
import org.hyperledger.bpa.controller.api.partner.PartnerCredentialType;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.repository.BPASchemaRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Singleton
public class PartnerCredDefLookup {

    @Value("${bpa.did.prefix}")
    @Setter(AccessLevel.PACKAGE)
    String didPrefix;

    @Inject
    @Setter(AccessLevel.PACKAGE)
    Optional<LedgerClient> ledger;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    @Setter(AccessLevel.PACKAGE)
    BPASchemaRepository schemaRepo;

    @Inject
    Converter conv;

    public List<PartnerAPI> getIssuersFor(@NonNull String schemaId) {
        List<PartnerAPI> result = new ArrayList<>();

        // TODO filter

        schemaRepo.findBySchemaId(schemaId)
                .ifPresent(s -> partnerRepo.findBySupportedCredential(s.getSeqNo().toString()).forEach(
                        dbPartner -> result.add(conv.toAPIObject(dbPartner))));
        return result;
    }

    @Scheduled(cron = "0 15 2 ? * *")
    public void lookupTypesForAllPartners() {
        ledger.ifPresent(l -> {
            Map<String, List<PartnerCredentialType>> didToTypes = new HashMap<>();
            schemaRepo.findAll().forEach(
                    s -> l.queryCredentialDefinitions(s.getSeqNo().toString()).ifPresent(defs -> defs.forEach(def -> {
                        String did = AriesStringUtil.credDefIdGetDid(def.getCredentialDefinitionId());
                        if (didToTypes.containsKey(did)) {
                            didToTypes.get(did).add(def);
                        } else {
                            List<PartnerCredentialType> types = new ArrayList<>();
                            types.add(def);
                            didToTypes.put(did, types);
                        }
                    })));
            didToTypes.forEach(
                    (did, types) -> partnerRepo.updateByDid(didPrefix + did,
                            conv.toMap(new CredentialTypeWrapper(types))));
        });
    }

    @Async
    public void lookupTypesForAllPartnersAsync() {
        lookupTypesForAllPartners();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static final class CredentialTypeWrapper {
        List<PartnerCredentialType> wrapped;
    }
}
