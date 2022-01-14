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

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.exceptions.DataAccessException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.api.TagAPI;
import org.hyperledger.bpa.api.aries.TransactionRole;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.config.RuntimeConfig;
import org.hyperledger.bpa.config.TagConfig;
import org.hyperledger.bpa.model.Tag;
import org.hyperledger.bpa.repository.TagRepository;

import java.util.*;

@Slf4j
@Singleton
public class TagService {

    @Inject
    TagRepository tagRepo;

    @Inject
    RuntimeConfig config;

    @Inject
    TagConfig configuredTags;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    public @Nullable TagAPI addTag(@NonNull String name) {
        return addTag(name, Boolean.FALSE);
    }

    TagAPI addTag(@NonNull String name, @NonNull Boolean isReadOnly) {
        Tag tag = Tag.builder()
                .name(name)
                .isReadOnly(isReadOnly)
                .build();

        Tag saved;
        try {
            saved = tagRepo.save(tag);
        } catch (DataAccessException e) {
            throw new WrongApiUsageException(ms.getMessage("api.tag.already.exists", Map.of("name", name)));
        }

        return TagAPI.from(saved);
    }

    public TagAPI updateTag(@NonNull UUID id, @NonNull String name) {
        Tag dbTag = tagRepo.findById(id).orElseThrow(EntityNotFoundException::new);
        tagRepo.updateNameById(dbTag.getId(), name);
        log.debug("Updating existing tag name {} with new name: {}", dbTag, name);
        dbTag.setName(name);
        return TagAPI.from(dbTag);
    }

    public List<TagAPI> listTags() {
        List<TagAPI> result = new ArrayList<>();
        tagRepo.findAll().forEach(dbT -> result.add(TagAPI.from(dbT)));
        return result;
    }

    public Optional<TagAPI> getTag(@NonNull UUID id) {
        Optional<Tag> tag = tagRepo.findById(id);
        return tag.map(TagAPI::from);
    }

    public void deleteTag(@NonNull UUID id, @Nullable boolean force) {
        int refs = tagRepo.countReferencesToPartner(id);
        if (!force && refs > 0) {
            throw new WrongApiUsageException(ms.getMessage("api.tag.constraint.violation", Map.of("count", refs)));
        }
        tagRepo.findById(id).ifPresent(s -> tagRepo.deleteByTagId(id));
    }

    public void createDefaultTags() {
        if (configuredTags.getTags() != null) {
            configuredTags.getTags().forEach(t -> {
                try {
                    if (tagRepo.contByName(t) == 0) {
                        TagAPI tagAPI = addTag(t, Boolean.TRUE);
                        log.debug("Added preconfigured tag with name: {}", tagAPI.getName());
                    }
                } catch (DataAccessException e) {
                    log.warn("Tag with name {} already exists", t, e);
                }
            });
        }

        // if an endorser/autor role is explicitely set ...
        if (TransactionRole.ENDORSER.name().equalsIgnoreCase(config.getEndorserRole())) {
            // if BPA is an Endorser, can set connected partners as Authors
            if (tagRepo.contByName(TransactionRole.AUTHOR.name()) == 0) {
                addTag(TransactionRole.AUTHOR.name());
            }
        } else if (TransactionRole.AUTHOR.name().equalsIgnoreCase(config.getEndorserRole())) {
            // if BPA is an Author, can set connected partners as Endorsers
            if (tagRepo.contByName(TransactionRole.ENDORSER.name()) == 0) {
                addTag(TransactionRole.ENDORSER.name());
            }
        }
    }
}
