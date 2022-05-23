package org.hyperledger.bpa.impl.rules;
import io.micronaut.context.ApplicationContext;
import lombok.Builder;
import lombok.Data;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.bpa.persistence.model.Partner;

@Data
@Builder
public class EventContext {
    private Partner partner;
    private PresentationExchangeRecord presEx;
    private ConnectionRecord connRec;
    private ApplicationContext ctx;
}
