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
package org.hyperledger.bpa.impl.aries.prooftemplates;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.exceptions.DataAccessException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.bpa.api.exception.DataPersistenceException;
import org.hyperledger.bpa.api.exception.ProofTemplateException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.aries.proof.ProofManager;
import org.hyperledger.bpa.model.BPAProofTemplate;
import org.hyperledger.bpa.model.prooftemplate.ValueOperators;
import org.hyperledger.bpa.repository.BPAProofTemplateRepository;

import java.util.*;
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

    public void invokeProofRequestByTemplate(@NonNull UUID id, @NonNull UUID partnerId) {
        invokeProofRequestByTemplate(id, partnerId, null);
    }

    public void invokeProofRequestByTemplate(@NonNull UUID id, @NonNull UUID partnerId,
            @Nullable ExchangeVersion version) {
        BPAProofTemplate proofTemplate = repo.findById(id)
                .orElseThrow(() -> new ProofTemplateException(
                        ms.getMessage("api.proof.template.not.found", Map.of("id", id))));
        version = version != null ? version : ExchangeVersion.V1;
        proofManager.sendPresentProofRequest(partnerId, proofTemplate, version);
    }

    public Optional<BPAProofTemplate> getProofTemplate(@NonNull UUID id) {
        return repo.findById(id);
    }

    public BPAProofTemplate addProofTemplate(@NonNull BPAProofTemplate template) {
        return repo.save(template);
    }

    public Stream<BPAProofTemplate> listProofTemplates() {
        return StreamSupport.stream(repo.findAll().spliterator(), false);
    }

    public void removeProofTemplate(@NonNull UUID templateId) {
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
