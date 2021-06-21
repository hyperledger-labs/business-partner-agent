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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.api.TagAPI;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.TagConfig;
import org.hyperledger.bpa.model.Tag;
import org.hyperledger.bpa.repository.TagRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Slf4j
@Singleton
public class TagService {

    @Inject
    TagRepository tagRepo;

    @Inject
    List<TagConfig> configuredTags;

    // CRUD Methods
    public @Nullable TagAPI addTag(@NonNull String name) {
        return addTag(name, false);
    }

    @Nullable
    TagAPI addTag(@NonNull String name, boolean isReadOnly) {
        TagAPI result;

        if (tagRepo.findByName(name).isPresent()) {
            throw new WrongApiUsageException("Tag with name: " + name + " already exists.");
        }
        Tag dbT = Tag.builder()
                .name(name)
                .isReadOnly(isReadOnly)
                .build();

        Tag saved = tagRepo.save(dbT);
        result = TagAPI.from(saved);

        return result;
    }

    public Optional<TagAPI> updateTag(@NonNull UUID id) {
        Optional<Tag> tag = tagRepo.findById(id);
        log.debug("{}", tag);
        // TODO: Implement
        return Optional.empty();
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

    public void deleteTag(@NonNull UUID id) {
        tagRepo.findById(id).ifPresent(s -> {
            tagRepo.deleteById(id);
            // TODO: Clean up. What happens to referenced tags in partners and rules?
        });
    }

    public Optional<Tag> getTagFor(@NonNull String name) {
        return tagRepo.findByName(name);
    }

    public void resetWriteOnlyTags() {
        tagRepo.deleteByIsReadOnly(Boolean.TRUE);

        for (TagConfig tag : configuredTags) {
            try {
                TagAPI tagAPI = addTag(tag.getName(), true);
                log.debug("{}", tagAPI);

            } catch (Exception e) {
                if (e instanceof WrongApiUsageException) {
                    log.warn("Tag already exists: {}", tag.getName());
                } else {
                    log.warn("Could not add tag: {}", tag.getName(), e);
                }

            }
        }
    }
}
