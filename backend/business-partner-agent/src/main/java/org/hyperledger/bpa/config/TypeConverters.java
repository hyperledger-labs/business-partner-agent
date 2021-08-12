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
package org.hyperledger.bpa.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.convert.TypeConverter;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.model.prooftemplate.BPAAttributeGroup;
import org.hyperledger.bpa.model.prooftemplate.BPAAttributeGroups;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Slf4j
@Factory
public class TypeConverters {

    public static final TypeReference<List<BPAAttributeGroup>> ATTR_REF = new TypeReference<>() {
    };

    @Inject
    ObjectMapper mapper;

    @Singleton
    TypeConverter<BPAAttributeGroups, String> attrsToString() {
        return (object, targetType, context) -> Optional.ofNullable(attributeToString(object));
    }

    @Singleton
    TypeConverter<String, BPAAttributeGroups> stringToAttrs() {
        return (object, targetType, context) -> Optional.ofNullable(stringToAttribute(object));
    }

    private String attributeToString(BPAAttributeGroups f) {
        String res = null;
        try {
            res = mapper.writeValueAsString(f);
        } catch (JsonProcessingException e) {
            log.error("could not convert to json: ", e);
        }
        return res;
    }

    private BPAAttributeGroups stringToAttribute(String f) {
        BPAAttributeGroups res = null;
        try {
            res = mapper.readValue(f, BPAAttributeGroups.class);
        } catch (JsonProcessingException e) {
            log.error("could not convert from json: {}", f, e);
        }
        return res;
    }
}
