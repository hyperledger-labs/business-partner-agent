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
import org.hyperledger.bpa.impl.ProofTemplateManager;
import org.hyperledger.bpa.impl.verification.prooftemplates.ValidUUID;
import org.hyperledger.bpa.model.BPAProofTemplate;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

@Controller("/api/proof-templates")
@Tag(name = "Proof Template Management")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class ProofTemplateController {

    @Inject
    private ProofTemplateManager proofTemplateManager;

    @Get
    public HttpResponse<List<ProofTemplate>> listProofTemplates() {
        return HttpResponse.ok(
                proofTemplateManager.listProofTemplates()
                        .map(BPAProofTemplate::toRepresentation)
                        .collect(Collectors.toList()));
    }

    @Post
    public HttpResponse<ProofTemplate> addProofTemplate(@Valid ProofTemplate template) {
        if (template.getId() == null) {
            BPAProofTemplate persistedTemplate = proofTemplateManager
                    .addProofTemplate(BPAProofTemplate.fromRepresentation(template));
            return HttpResponse.created(persistedTemplate.toRepresentation());
        } else {
            return HttpResponse.badRequest(template);
        }
    }

    // TODO add possibility to update a template, because we might refer to
    // templates via FK, updates have to create new entities.

    @Get("/known-condition-operators")
    public HttpResponse<Set<String>> listKnownConditionOperators() {
        return HttpResponse.ok(proofTemplateManager.getKnownConditionOperators());
    }

    @Put("/{id}/ProofRequest/{partnerId}")
    public HttpResponse<Void> invokeProofRequestByTemplate(
            @PathVariable @ValidUUID @NotNull String id,
            @PathVariable @ValidUUID @NotNull String partnerId) {
        proofTemplateManager.invokeProofRequestByTemplate(UUID.fromString(id), UUID.fromString(partnerId));
        return HttpResponse.ok();
    }

    @Delete("/{id}")
    public HttpResponse<Void> removeProofTemplate(@PathVariable @ValidUUID @NotNull String id) {
        proofTemplateManager.removeProofTemplate(UUID.fromString(id));
        return HttpResponse.ok();
    }
}
