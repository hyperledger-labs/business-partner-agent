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
package org.hyperledger.oa.registry.config;

import javax.inject.Singleton;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ConnectionPoolSettings;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.core.util.StringUtils;

@Factory
public class MongoDBConfig extends AbstractMongoDBConfig {

    @Value("${mongodb.uri}")
    private String connectionString;

    @Singleton
    @Bean(preDestroy = "close")
    @Requires(notEnv = { Environment.TEST })
    public MongoClient mongoClient() {
        if (StringUtils.isEmpty(connectionString)) {
            throw new IllegalStateException("MongoDB connection string must not be empty, check if "
                    + "mongodb.uri is set correctly.");
        }
        MongoClientSettings.Builder settings = MongoClientSettings.builder()
                .writeConcern(WriteConcern.ACKNOWLEDGED)
                .retryWrites(true)
                .applyToConnectionPoolSettings(builder -> builder.applySettings(ConnectionPoolSettings.builder()
                        .maxSize(20)
                        .build()))
                .applyConnectionString(new ConnectionString(connectionString))
                .codecRegistry(createCodecRegistry());
        return MongoClients.create(settings.build());
    }
}
