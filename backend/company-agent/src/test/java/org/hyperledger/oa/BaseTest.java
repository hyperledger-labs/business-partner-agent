/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa;

import org.hyperledger.oa.util.FileLoader;
import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base Class for none Micronaut Tests
 */
public abstract class BaseTest {

    protected ObjectMapper mapper = new ObjectMapper();

    protected FileLoader loader = FileLoader.newLoader();

    @BeforeEach
    void setup() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public <T> T loadAndConvertTo(String file, Class<T> type) throws JsonProcessingException {
        String didDocument = loader.load(file);
        return mapper.readValue(didDocument, type);
    }

    public <T> T loadAndConvertTo(String file, TypeReference<T> type) throws JsonProcessingException {
        String didDocument = loader.load(file);
        return mapper.readValue(didDocument, type);
    }

}
