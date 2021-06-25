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
package org.hyperledger.bpa.repository;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import org.hyperledger.bpa.model.Tag;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface TagRepository extends CrudRepository<Tag, UUID> {

    @Join(value = "partners", type = Join.Type.LEFT_FETCH)
    Optional<Tag> findByName(String name);

    @Override
    @Query("delete from partner_tag where tag_id = :id; delete from tag where id = :id")
    void deleteById(@NonNull UUID id);

    void updateNameById(@Id UUID id, String name);

    /** Deletes partner to tag mapping from the mapping table */
    @Query("delete from partner_tag where tag_id = :tagId and partner_id = :partnerId")
    void deletePartnerToTagMapping(@NonNull UUID partnerId, @NonNull UUID tagId);

    @Query("delete from partner_tag where partner_id = :partnerId")
    void deleteAllPartnerToTagMappings(@NonNull UUID partnerId);

    @Query("insert into partner_tag (partner_id, tag_id) values (:partnerId, :tagId)")
    void createPartnerToTagMapping(@NonNull UUID partnerId, @NonNull UUID tagId);

    @Query("select count(tag_id) from partner_tag where tag_id = :tagId")
    int countReferencesToPartner(@NonNull UUID tagId);

    @Query("select count(*) from tag where name = :name")
    int contByName(@NonNull String name);

    default void updateAllPartnerToTagMappings(@lombok.NonNull UUID partnerId, @Nullable Collection<Tag> mappings) {
        deleteAllPartnerToTagMappings(partnerId);
        if (CollectionUtils.isNotEmpty(mappings)) {
            mappings.forEach(m -> {
                if (m.getId() == null) {
                    Tag saved = save(Tag.builder()
                            .name(m.getName())
                            .isReadOnly(Boolean.FALSE)
                            .build());
                    m.setId(saved.getId());
                }
                createPartnerToTagMapping(partnerId, m.getId());
            });
        }
    }
}
