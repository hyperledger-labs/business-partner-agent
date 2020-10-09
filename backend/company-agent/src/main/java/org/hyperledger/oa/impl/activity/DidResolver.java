package org.hyperledger.oa.impl.activity;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hyperledger.oa.api.DidDocAPI;
import org.hyperledger.oa.api.PartnerAPI;
import org.hyperledger.oa.api.exception.PartnerException;
import org.hyperledger.oa.client.URClient;
import org.hyperledger.oa.impl.util.AriesStringUtil;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.PartnerProof;
import org.hyperledger.oa.repository.PartnerRepository;

import io.micronaut.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;

/**
 * Special usecase to resolve a partners public did and profile
 * in case of an incoming connection where the partners public did is not known.
 */
@Slf4j
@Singleton
public class DidResolver {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    PartnerLookup partnerLookup;

    @Inject
    URClient ur;

    @Inject
    Converter converter;

    @Async
    public void resolveDid(PartnerProof pp) {
        try {
            if (StringUtils.isNotEmpty(pp.getSchemaId())
                    && AriesStringUtil.schemaGetName(pp.getSchemaId()).equals("commercialregister")) {
                partnerRepo.findById(pp.getPartnerId()).ifPresent(p -> {
                    if (p.getVerifiablePresentation() == null
                            && p.getIncoming() != null
                            && p.getIncoming().booleanValue() == true) {
                        Optional<DidDocAPI> didDocument = Optional.empty();
                        try {
                            didDocument = ur.getDidDocument(p.getDid());
                        } catch (PartnerException e) {
                            log.error("{}", e.getMessage());
                        }
                        if (didDocument.isEmpty() && pp.getProof() != null) {
                            Object pubDid = pp.getProof().get("did");
                            if (pubDid != null) {
                                log.debug("Resolved did: {}", pubDid);
                                final PartnerAPI pAPI = partnerLookup.lookupPartner(pubDid.toString());
                                p.setDid(pubDid.toString());
                                p.setValid(pAPI.getValid());
                                p.setVerifiablePresentation(converter.toMap(pAPI.getVerifiablePresentation()));
                                partnerRepo.update(p);
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            log.error("Could not lookup public did.", e);
        }
    }
}
