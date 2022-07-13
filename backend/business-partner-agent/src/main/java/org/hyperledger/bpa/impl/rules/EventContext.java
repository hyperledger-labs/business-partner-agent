package org.hyperledger.bpa.impl.rules;

import io.micronaut.context.ApplicationContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Builder;
import lombok.Data;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.bpa.impl.PartnerManager;
import org.hyperledger.bpa.impl.TagService;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;
import org.hyperledger.bpa.persistence.repository.TagRepository;

@Data
@Builder
public class EventContext {
    private Partner partner;
    private PresentationExchangeRecord presEx;
    private ConnectionRecord connRec;
    private ApplicationContext ctx;
    private PartnerRepository partnerRepo;
    private TagService tagService;
    private TagRepository tagRepo;
    private PartnerManager partnerManager;
}
