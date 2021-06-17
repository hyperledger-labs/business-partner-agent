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
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hyperledger.bpa.controller.api.prooftemplates.ProofTemplate;

import java.util.*;

@Controller("/api/proof-templates")
@Tag(name = "Proof Template Management")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class ProofTemplateController {
    @Deprecated
    private Map<String, ProofTemplate> inMemory = new HashMap<>();

    @Get
        public HttpResponse<List<ProofTemplate>> listProofTemplates() {
        return HttpResponse.ok(new ArrayList<>(this.inMemory.values()));
    }

    @Post
    public HttpResponse<ProofTemplate> addProofTemplate(ProofTemplate template) {
        if (template.getId() != null) {
            String newId = UUID.randomUUID().toString();
            template.setId(newId);
            inMemory.put(newId, template);
        }else{
            return HttpResponse.badRequest(template);
        }
        return HttpResponse.created(template);
    }

    @Delete("/{id}")
    public HttpResponse<Void> addProofTemplate(@PathVariable String id) {
        inMemory.remove(id);
        return HttpResponse.ok();
    }
}
