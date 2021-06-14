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
package org.hyperledger.bpa.impl.rules.definitions;

import com.deliveredtechnologies.rulebook.NameValueReferableTypeConvertibleMap;
import com.deliveredtechnologies.rulebook.lang.RuleBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.connection.ConnectionState;

import java.util.UUID;

@Slf4j
@Data @EqualsAndHashCode(callSuper = true)
public class AssertVerifiedConnection extends BaseRule {

    private final UUID partnerId;
    private final UUID proofTemplateId;

    public AssertVerifiedConnection(@NonNull UUID partnerId, @NonNull UUID proofTemplateId) {
        super(UUID.randomUUID(), Run.ONCE);
        this.partnerId = partnerId;
        this.proofTemplateId = proofTemplateId;
    }

    @Override
    public void defineRules() {
        addRule(RuleBuilder.create().withFactType(EventContext.class)
                .when(this::isConnection)
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

    private boolean isConnection(NameValueReferableTypeConvertibleMap<EventContext> facts) {
        EventContext f = facts.getOne();
        return f.getConnRec() != null
                && ConnectionState.REQUEST.equals(f.getConnRec().getState())
                && f.getPartner() != null
                && partnerId.equals(f.getPartner().getId());
    }

    private static boolean isPresentationExchange(NameValueReferableTypeConvertibleMap<EventContext> facts) {
        EventContext f = facts.getOne();
        return f.getPresEx() != null;
    }

}
