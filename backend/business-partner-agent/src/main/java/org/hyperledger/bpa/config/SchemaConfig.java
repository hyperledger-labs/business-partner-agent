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
package org.hyperledger.bpa.config;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import lombok.Data;
import org.hyperledger.bpa.api.CredentialType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Schema configuration bean that maps the default schema configuration. see:
 * schemas.yml
 */
@EachProperty("bpa.schemas")
@Data
public class SchemaConfig {

    /**
     * Bean name
     */
    private String name;

    private CredentialType type;

    private String label;
    private String id;
    private String defaultAttributeName;
    // Generic structure - [{key: value, key: value}, {key: value}]
    private List<Map<String, String>> restrictions;

    // json-ld based schemas only
    private String ldType;
    private Set<String> attributes;

    public SchemaConfig(@Parameter String name) {
        this.name = name;
    }
}