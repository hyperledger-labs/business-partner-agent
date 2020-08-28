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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@Requires(env = { Environment.TEST })
public class MongoDBTestContainer {

    @Getter
    private Integer mappedPort;
    private GenericContainer<?> mongoDBContainer;
    private final Integer mongoDBPort = Integer.valueOf(27017);

    @SuppressWarnings("resource")
    @PostConstruct
    public void startDB() {
        mongoDBContainer = new GenericContainer<>("mongo")
                .withExposedPorts(mongoDBPort);
        mongoDBContainer.setWaitStrategy(Wait.defaultWaitStrategy());
        mongoDBContainer.start();

        mappedPort = mongoDBContainer.getMappedPort(mongoDBPort.intValue());

        log.debug("Running with docker mongodb and port: {}", mappedPort);
    }

    @PreDestroy
    public void close() {
        if (mongoDBContainer != null) {
            mongoDBContainer.close();
            mongoDBContainer.stop();
        }
    }
}
