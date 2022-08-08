/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.controller.api.prooftemplates.ProofTemplate;
import org.hyperledger.bpa.impl.aries.prooftemplates.ProofTemplateManager;
import org.hyperledger.bpa.persistence.model.BPAProofTemplate;
import org.hyperledger.bpa.persistence.model.prooftemplate.ValueOperators;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller("/api/proof-templates")
@Tag(name = "Proof Template Management")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class ProofTemplateController {

    @Inject
    ProofTemplateManager proofTemplateManager;

    /**
     * List configured templates
     *
     * @return list of {@link ProofTemplate}
     */
    @Get
    public HttpResponse<List<ProofTemplate>> listProofTemplates() {
        return HttpResponse.ok(
                proofTemplateManager.listProofTemplates()
                        .map(BPAProofTemplate::toRepresentation)
                        .collect(Collectors.toList()));
    }

    /**
     * Get template by id
     *
     * @param id proof template id
     * @return {@link ProofTemplate}
     */
    @Get("/{id}")
    public HttpResponse<ProofTemplate> getProofTemplateForId(@PathVariable UUID id) {
        return HttpResponse.ok(proofTemplateManager.findProofTemplate(id).toRepresentation());
    }

    /**
     * Add a new proof template
     *
     * @param template {@link ProofTemplate}
     * @return {@link ProofTemplate}
     */
    @Post
    public HttpResponse<ProofTemplate> addProofTemplate(@Valid @Body ProofTemplate template) {
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

    /**
     * List configured proof condition operators
     *
     * @param type {@link CredentialType} filter operators by type
     * @return list of {@link ValueOperators}
     */
    @Get("/known-condition-operators")
    public HttpResponse<Set<String>> listKnownConditionOperators(@Nullable @QueryValue CredentialType type) {
        return HttpResponse.ok(proofTemplateManager.getKnownConditionOperators(type));
    }

    /**
     * Delete proof template by id
     *
     * @param id proof template id
     * @return Http Status
     */
    @Delete("/{id}")
    public HttpResponse<Void> removeProofTemplate(@PathVariable UUID id) {
        proofTemplateManager.removeProofTemplate(id);
        return HttpResponse.ok();
    }
}
