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
package org.hyperledger.bpa.impl.aries.jsonld;

import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.client.DidDocClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class SchemaContextResolverTest {

    @Mock
    DidDocClient http;

    @InjectMocks
    SchemaContextResolver res;

    @Test
    void testResolveTypeFromContext() {
        String ctx = """
                {
                    "@context": {
                        "PermanentResident": {
                            "@id": "https://w3id.org/citizenship#PermanentResident"
                        }
                    }
                }
                """;
        SchemaContextResolver.LDContext ldContext = GsonConfig.defaultConfig().fromJson(ctx,
                SchemaContextResolver.LDContext.class);
        Mockito.when(http.call(Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(ldContext));
        String id = res.resolve("https://dummy.test", "PermanentResident");
        Assertions.assertEquals("https://w3id.org/citizenship#PermanentResident", id);
    }

    @Test
    void testResolveTypeFromContextFails() {
        Mockito.when(http.call(Mockito.anyString(), Mockito.any())).thenReturn(Optional.empty());
        Assertions.assertThrows(IllegalStateException.class, () -> res.resolve("", ""));
    }
}
