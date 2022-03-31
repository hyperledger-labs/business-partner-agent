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
package org.hyperledger.bpa.controller.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.micronaut.data.model.Page;
import jakarta.inject.Singleton;

import java.io.IOException;

@Singleton
public class PageSerializer extends JsonSerializer<Page<?>> {
    @Override
    public void serialize(Page<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("pageNumber", value.getPageNumber());
        if (value.getNumberOfElements() != value.getSize()) {
            gen.writeNumberField("numberOfElements", value.getNumberOfElements());
        }
        gen.writeNumberField("size", value.getSize());
        gen.writeNumberField("totalPages", value.getTotalPages());
        gen.writeNumberField("totalSize", value.getTotalSize());
        gen.writeObjectField("content", value.getContent());
        gen.writeEndObject();
    }
}
