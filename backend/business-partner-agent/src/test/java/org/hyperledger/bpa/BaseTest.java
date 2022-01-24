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
package org.hyperledger.bpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.util.CollectionUtils;
import lombok.NonNull;
import org.hyperledger.bpa.impl.aries.jsonld.VPManager;
import org.hyperledger.bpa.testutil.FileLoader;
import org.junit.jupiter.api.BeforeEach;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.fail;

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

    public static void waitForVP(@NonNull VPManager vpMgmt, boolean waitForVC) throws Exception {
        Instant timeout = Instant.now().plusSeconds(30);
        while (vpMgmt.getVerifiablePresentation().isEmpty()
                || (waitForVC && vpMgmt.getVerifiablePresentation().get().getVerifiableCredential() == null)) {
            Thread.sleep(15);
            if (Instant.now().isAfter(timeout)) {
                fail("Timeout reached while waiting for the VP to be created");
            }
        }
    }

    public static void waitForVCDeletion(@NonNull VPManager vpMgmt) throws Exception {
        Instant timeout = Instant.now().plusSeconds(30);
        while (vpMgmt.getVerifiablePresentation().isPresent()
                && CollectionUtils.isNotEmpty(vpMgmt.getVerifiablePresentation().get().getVerifiableCredential())) {
            Thread.sleep(15);
            if (Instant.now().isAfter(timeout)) {
                fail("Timeout reached while waiting for the VP to be created");
            }
        }
    }
}
