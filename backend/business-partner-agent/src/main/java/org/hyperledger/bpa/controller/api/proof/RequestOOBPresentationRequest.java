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
package org.hyperledger.bpa.controller.api.proof;

import io.micronaut.core.annotation.Introspected;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.bpa.controller.api.ExchangeVersionTranslator;
import org.hyperledger.bpa.persistence.model.Tag;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Introspected
@Data
@NoArgsConstructor
public class RequestOOBPresentationRequest implements ExchangeVersionTranslator {

    // connection
    private String alias;
    private List<Tag> tag;
    private Boolean trustPing;

    // bpa internal id
    @NotNull
    private UUID templateId;

    private ExchangeVersion exchangeVersion;
}
