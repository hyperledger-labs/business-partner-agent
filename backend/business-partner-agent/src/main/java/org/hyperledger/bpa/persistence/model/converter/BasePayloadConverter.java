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
package org.hyperledger.bpa.persistence.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.data.model.runtime.convert.AttributeConverter;
import jakarta.inject.Inject;

public abstract class BasePayloadConverter<I, L> implements AttributeConverter<ExchangePayload<I, L>, String> {

    @Inject
    ObjectMapper mapper;

    @Override
    public String convertToPersistedValue(ExchangePayload<I, L> entityValue, @NonNull ConversionContext context) {
        if (entityValue == null) {
            return null;
        }
        try {
            if (entityValue.typeIsJsonLd()) {
                return mapper.writeValueAsString(entityValue.getLdProof());
            }
            return mapper.writeValueAsString(entityValue.getIndy());
        } catch (JsonProcessingException e) {
            throw new ConversionException("Could not serialise exchange record");
        }
    }

    public static final class ConversionException extends RuntimeException {
        public ConversionException(String message) {
            super(message);
        }
    }
}
