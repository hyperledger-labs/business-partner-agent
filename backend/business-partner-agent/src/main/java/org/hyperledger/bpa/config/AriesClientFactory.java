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
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import jakarta.inject.Singleton;
import okhttp3.OkHttpClient;
import org.hyperledger.aries.AriesClient;

import java.util.concurrent.TimeUnit;

@Factory
@Requires(notEnv = Environment.TEST)
public class AriesClientFactory {

    private static final long FIVE = 300;

    @Value("${bpa.acapy.url}")
    private String url;
    @Value("${bpa.acapy.apiKey}")
    private String apiKey;

    @Singleton
    public AriesClient ariesClient() {
        return AriesClient.builder()
                .url(url)
                .apiKey(apiKey)
                .client(new OkHttpClient.Builder()
                        .writeTimeout(FIVE, TimeUnit.SECONDS)
                        .readTimeout(FIVE, TimeUnit.SECONDS)
                        .connectTimeout(FIVE, TimeUnit.SECONDS)
                        .callTimeout(FIVE, TimeUnit.SECONDS)
                        .build())
                .build();
    }
}
