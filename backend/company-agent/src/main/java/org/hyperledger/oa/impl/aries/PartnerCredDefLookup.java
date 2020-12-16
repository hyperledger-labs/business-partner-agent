package org.hyperledger.oa.impl.aries;

import io.micronaut.context.annotation.Value;
import io.micronaut.scheduling.annotation.Async;
import io.micronaut.scheduling.annotation.Scheduled;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.oa.api.PartnerAPI;
import org.hyperledger.oa.client.LedgerClient;
import org.hyperledger.oa.controller.api.partner.PartnerCredentialType;
import org.hyperledger.oa.impl.util.AriesStringUtil;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.Partner;
import org.hyperledger.oa.repository.PartnerRepository;
import org.hyperledger.oa.repository.SchemaRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class PartnerCredDefLookup {

    @Value("${oagent.did.prefix}")
    @Setter(AccessLevel.PACKAGE)
    String didPrefix;

    @Inject
    @Setter(AccessLevel.PACKAGE)
    LedgerClient ledger;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    @Setter(AccessLevel.PACKAGE)
    SchemaRepository schemaRepo;

    @Inject
    Converter conv;

    public Optional<List<PartnerCredentialType>> getPartnerCredDefs(@NonNull UUID partnerId) {
        Optional<List<PartnerCredentialType>> result = Optional.empty();
        final Optional<Partner> p = partnerRepo.findById(partnerId);
        if (p.isPresent() && StringUtils.isNotEmpty(p.get().getDid())) {
            result = ledger.queryCredentialDefinitions(p.get().getDid());
        }
        return result;
    }

    Optional<String> findCredentialDefinitionId(@NonNull UUID partnerId, @NonNull Integer seqNo) {
        Optional<String> result = Optional.empty();

        final Optional<List<PartnerCredentialType>> pct = getPartnerCredDefs(partnerId);
        if (pct.isPresent()) {
            final List<PartnerCredentialType> types = pct.get().stream()
                    .filter(cred -> AriesStringUtil.credDefIdGetSquenceNo(
                            cred.getCredentialDefinitionId()).equals(seqNo.toString()))
                    .collect(Collectors.toList());
            if (types.size() > 0) {
                result = Optional.of(types.get(types.size() - 1).getCredentialDefinitionId());
            }
        }
        return result;
    }

    public List<PartnerAPI> getIssuersFor(@NonNull String schemaId) {
        List<PartnerAPI> result = new ArrayList<>();
        schemaRepo.findBySchemaId(schemaId)
                .ifPresent(s -> partnerRepo.findBySupportedCredential(s.getSeqNo().toString()).forEach(
                        dbPartner -> result.add(conv.toAPIObject(dbPartner))));
        return result;
    }

    @Scheduled(cron = "0 15 2 ? * *")
    public void lookupTypesForAllPartners() {
        Map<String, List<PartnerCredentialType>> didToTypes = new HashMap<>();
        schemaRepo.findAll().forEach(
                s -> ledger.queryCredentialDefinitions(s.getSeqNo().toString()).ifPresent(defs -> defs.forEach(def -> {
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
                (did, types) -> partnerRepo.updateByDid(didPrefix + did, conv.toMap(new CredentialTypeWrapper(types))));
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
