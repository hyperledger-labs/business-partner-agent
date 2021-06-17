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
package org.hyperledger.bpa.impl.rulesold;

import com.deliveredtechnologies.rulebook.FactMap;
import com.deliveredtechnologies.rulebook.NameValueReferableMap;
import io.micronaut.context.ApplicationContext;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.webhook.EventHandler;
import org.hyperledger.bpa.impl.rulesold.definitions.BaseRule;
import org.hyperledger.bpa.impl.rulesold.definitions.EventContext;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import java.util.List;

@Slf4j
// @Singleton
public class RulesEventHandler extends EventHandler {

    @Inject
    RulesService ts;

    @Inject
    PartnerRepository pr;

    @Inject
    ApplicationContext appCtx;

    @Override
    public void handleConnection(ConnectionRecord connection) {
        pr.findByConnectionId(connection.getConnectionId()).ifPresent(p -> {
            NameValueReferableMap<EventContext> facts = new FactMap<>();
            facts.setValue("connection", EventContext
                    .builder()
                    .partner(p)
                    .connRec(connection)
                    .ctx(appCtx)
                    .build());
            runAndHandleResult(facts);
        });
    }

    @Override
    public void handleProof(PresentationExchangeRecord presEx) {
        pr.findByConnectionId(presEx.getConnectionId()).ifPresent(p -> {
            NameValueReferableMap<EventContext> facts = new FactMap<>();
            facts.setValue("presentation", EventContext
                    .builder()
                    .partner(p)
                    .presEx(presEx)
                    .ctx(appCtx)
                    .build());
            runAndHandleResult(facts);
        });
    }

    private void runAndHandleResult(NameValueReferableMap<EventContext> facts) {
        List<BaseRule> tasks = ts.getTasks();
        log.debug("Checking {} rules", tasks.size());
        tasks.parallelStream().forEach(t -> {
            t.run(facts);
            t.getResult().ifPresentOrElse(result -> {
                log.debug("Task: {}, Result: {}", t.getTaskId(), result.getValue());
                if (result.getValue() && BaseRule.Run.ONCE.equals(t.getRun())) {
                    log.debug("Tasks runs: {}, scheduling for removal", t.getRun());
                    ts.remove(t.getTaskId());
                }
            }, () -> log.warn("Task did return a result"));
        });
    }
}
