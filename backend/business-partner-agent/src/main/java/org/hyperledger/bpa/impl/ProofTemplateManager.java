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

package org.hyperledger.bpa.impl;

import io.micronaut.data.exceptions.DataAccessException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import org.hyperledger.bpa.api.exception.DataPersistenceException;
import org.hyperledger.bpa.api.exception.ProofTemplateException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.aries.ProofManager;
import org.hyperledger.bpa.model.BPAProofTemplate;
import org.hyperledger.bpa.model.prooftemplate.ValueOperators;
import org.hyperledger.bpa.repository.BPAProofTemplateRepository;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@AllArgsConstructor
@Singleton
public class ProofTemplateManager {

    @Inject
    BPAProofTemplateRepository repo;

    @Inject
    ProofManager proofManager;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    public void invokeProofRequestByTemplate(@NotNull UUID id, @NotNull UUID partnerId) {
        Optional<BPAProofTemplate> proofTemplate = repo.findById(id);
        proofTemplate.ifPresent(t -> proofManager.sendPresentProofRequest(partnerId, t));
        proofTemplate.orElseThrow(() -> new ProofTemplateException("No proof template found for: " + id));
    }

    public Optional<BPAProofTemplate> getProofTemplate(UUID id) {
        return repo.findById(id);
    }

    public BPAProofTemplate addProofTemplate(BPAProofTemplate template) {
        return repo.save(template);
    }

    public Stream<BPAProofTemplate> listProofTemplates() {
        return StreamSupport.stream(repo.findAll().spliterator(), false);
    }

    public void removeProofTemplate(UUID templateId) {
        try {
            repo.deleteById(templateId);
        } catch (DataAccessException e) {
            throw new DataPersistenceException(ms.getMessage("api.proof.template.constraint.violation"));
        }
    }

    public Set<String> getKnownConditionOperators() {
        return Arrays.stream(ValueOperators.values()).map(ValueOperators::getValue).collect(Collectors.toSet());
    }
}
