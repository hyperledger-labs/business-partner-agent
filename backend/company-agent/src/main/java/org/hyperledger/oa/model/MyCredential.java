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
package org.hyperledger.oa.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hyperledger.oa.api.CredentialType;

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * MyCredential is a credential that is received via aries. It is NOT to be
 * confused with the verifiable credential (VC) that is part of the public
 * profile. When a aries credential is made public it will become a VC as part
 * of the public profile.
 *
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Data
@Entity
public class MyCredential {

    @Id
    @AutoPopulated
    private UUID id;

    @Nullable
    private Instant issuedAt;

    // No update date here, if a credential needs to change a new one is issued

    private CredentialType type;

    private Boolean isPublic;

    @Nullable
    private String referent;

    private String connectionId;

    private String state;

    private String threadId;

    @Nullable
    private String label;

    @Nullable
    @TypeDef(type = DataType.JSON)
    private Map<String, Object> credential;
}
