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
package org.hyperledger.bpa.impl.rules;

import io.micronaut.context.ApplicationContext;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.webhook.EventHandler;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

@Slf4j
@Singleton
public class RulesEventHandler extends EventHandler {

    @Inject
    RulesService ts;

    @Inject
    PartnerRepository pr;

    @Inject
    ApplicationContext appCtx;

    @Override
    public void handleConnection(ConnectionRecord connection) {
        pr.findByConnectionId(connection.getConnectionId()).ifPresent(p -> runRule(EventContext
                .builder()
                .partner(p)
                .connRec(connection)
                .ctx(appCtx)
                .build()));
    }

    @Override
    public void handleProof(PresentationExchangeRecord presEx) {
        pr.findByConnectionId(presEx.getConnectionId()).ifPresent(p -> runRule(EventContext
                .builder()
                .partner(p)
                .presEx(presEx)
                .ctx(appCtx)
                .build()));
    }

    public void runRule(EventContext ctx) {
        List<RulesData> rules = ts.getAll();
        log.debug("Running event against {} active rules", rules.size());
        rules.parallelStream().forEach(r -> {
            if (r.getTrigger().apply(ctx)) {
                log.debug("Run rule with id: {}", r.getRuleId());
                r.getAction().run(ctx);
            }
        });
    }
}
