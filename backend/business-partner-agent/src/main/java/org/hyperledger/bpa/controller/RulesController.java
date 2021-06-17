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
package org.hyperledger.bpa.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hyperledger.bpa.controller.api.rules.RuleRequest;
import org.hyperledger.bpa.impl.rules.RulesData;
import org.hyperledger.bpa.impl.rules.RulesService;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller("/api/rules")
@Tag(name = "Rules")
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class RulesController {

    @Inject
    RulesService rules;

    /**
     * Add new rule with trigger and action
     * @param ruleRequest {@link RuleRequest}
     * @return {@link RulesData}
     */
    @Post
    public HttpResponse<RulesData> addRule(@Body RuleRequest ruleRequest) {
        return HttpResponse.ok(rules.register(ruleRequest.getTrigger(), ruleRequest.getAction()));
    }

    /**
     * Update rule
     * @param id {@link UUID} rule id
     * @param ruleRequest {@link RuleRequest}
     * @return {@link RulesData}
     */
    @Put("/{id}")
    public HttpResponse<RulesData> updateRule(@PathVariable String id, @Body RuleRequest ruleRequest) {
        Optional<RulesData> updated = rules.update(
                UUID.fromString(id), ruleRequest.getTrigger(), ruleRequest.getAction());
        if (updated.isPresent()) {
            return HttpResponse.ok(updated.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Dele rule by id
     * @param id {@link UUID} rule id
     * @return {@link HttpResponse}
     */
    @Delete("/{id}")
    public HttpResponse<Void> deleteRule(@PathVariable String id) {
        rules.remove(UUID.fromString(id));
        return HttpResponse.ok();
    }

    /**
     * List configured rules
     * @return list of {@link RulesData}
     */
    @Get
    public HttpResponse<List<RulesData>> listRules() {
        return HttpResponse.ok(rules.getRules());
    }

    /**
     * Get configured rule by id
     * @param id {@UUID} rule id
     * @return {@link RulesData}
     */
    @Get("/{id}")
    public HttpResponse<RulesData> getById(@PathVariable String id) {
        Optional<RulesData> rule = rules.getById(UUID.fromString(id));
        if (rule.isPresent()) {
            return HttpResponse.ok(rule.get());
        }
        return HttpResponse.notFound();
    }
}
