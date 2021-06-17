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
package org.hyperledger.bpa.impl.rulesold.definitions;

import com.deliveredtechnologies.rulebook.NameValueReferableTypeConvertibleMap;
import com.deliveredtechnologies.rulebook.lang.RuleBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.connection.ConnectionState;

import java.util.UUID;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class AssertVerifiedConnection extends BaseRule {

    private final UUID proofTemplateId;
    private final UUID partnerId;

    // todo keep state of connection request, and proof request

    /**
     * Rule that is not assigned to a partner/connection and therefore is always active
     * @param proofTemplateId {@link UUID}
     */
    public AssertVerifiedConnection(@NonNull UUID proofTemplateId) {
        super(UUID.randomUUID(), Run.MULTI);
        this.proofTemplateId = proofTemplateId;
        this.partnerId = null;
    }

    /**
     * Rule that is assigned to a partner and can be removed once it's been fulfilled.
     * @param proofTemplateId {@link UUID}
     * @param partnerId {@link UUID}
     */
    public AssertVerifiedConnection(@NonNull UUID proofTemplateId, @NonNull UUID partnerId) {
        super(UUID.randomUUID(), Run.ONCE);
        this.proofTemplateId = proofTemplateId;
        this.partnerId = partnerId;
    }

    @Override
    public void defineRules() {
        addRule(RuleBuilder.create().withFactType(EventContext.class)
                .when(this::shouldHandleConnection)
                .then((facts, result) -> {
                    // load template
                    // send proof request
                    log.debug("Is connection");
                    result.setValue(Boolean.FALSE);
                })
                .stop()
                .build());
        addRule(RuleBuilder.create().withFactType(EventContext.class)
                .when(AssertVerifiedConnection::isPresentationExchange)
                .then((facts, result) -> {
                    // check valid
                    // set label
                    log.debug("Is presentation");
                    result.reset();
                    result.setValue(Boolean.TRUE);
                })
                .stop()
                .build());
    }

    private boolean shouldHandleConnection(NameValueReferableTypeConvertibleMap<EventContext> facts) {
        EventContext f = facts.getOne();
        if (this.partnerId != null) {
            return f.getPartner() != null && f.getPartner().getId().equals(partnerId) && isConnection(facts);
        }
        return isConnection(facts);
    }

    private boolean isConnection(NameValueReferableTypeConvertibleMap<EventContext> facts) {
        EventContext f = facts.getOne();
        return f.getConnRec() != null
                && ConnectionState.REQUEST.equals(f.getConnRec().getState());
    }

    private static boolean isPresentationExchange(NameValueReferableTypeConvertibleMap<EventContext> facts) {
        EventContext f = facts.getOne();
        return f.getPresEx() != null;
    }

}
