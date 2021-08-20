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
package org.hyperledger.bpa.model;

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.bpa.api.CredentialType;

import io.micronaut.core.annotation.Nullable;
import org.hyperledger.bpa.api.aries.ExchangeVersion;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * MyCredential is a credential that is received via aries. It is NOT to be
 * confused with the verifiable credential (VC) that is part of the public
 * profile. When an aries credential is made public it will become a VC as part
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

    @Nullable
    @Enumerated(EnumType.STRING)
    private CredentialType type;

    private Boolean isPublic;

    /** the connection that issued the credential */
    @Nullable
    private String connectionId;

    /** aca-py credential identifier */
    @Nullable
    private String referent;

    /** temporary credential exchange identifier */
    @Nullable
    private String credentialExchangeId;

    @Enumerated(EnumType.STRING)
    private CredentialExchangeState state;

    @Nullable
    @Enumerated(EnumType.STRING)
    private ExchangeVersion exchangeVersion;

    private String threadId;

    @Nullable
    private String label;

    @Nullable
    private String issuer;

    @Nullable
    @TypeDef(type = DataType.JSON)
    private Map<String, Object> credential;

    @Nullable
    private Boolean revoked;

    public static MyCredential.MyCredentialBuilder defaultCredentialBuilder() {
        return MyCredential
                .builder()
                .isPublic(Boolean.FALSE)
                .type(CredentialType.INDY)
                .issuedAt(Instant.now());
    }
}
