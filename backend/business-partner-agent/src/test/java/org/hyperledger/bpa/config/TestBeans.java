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

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.bpa.RunWithAries;
import org.mockito.Mockito;

import javax.inject.Singleton;

@Factory
@Requires(env = Environment.TEST)
public class TestBeans {

    /**
     * Needs to be injected individually via {@link RunWithAries}
     *
     * @return {@link AriesClient}
     */
    @Singleton
    AriesClient getAriesClient() {
        return Mockito.mock(AriesClient.class);
    }
}
