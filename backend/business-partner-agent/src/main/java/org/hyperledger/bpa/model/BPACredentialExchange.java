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

import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import lombok.*;
import lombok.experimental.Accessors;
import org.hyperledger.acy_py.generated.model.CredAttrSpec;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.aries.ExchangeVersion;

import javax.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stores issued credentials
 *
 * TODO is basically the same as {@link MyCredential} and both can be merged
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
@Accessors(chain = true)
@Entity
@Table(name = "bpa_credential_exchange")
public class BPACredentialExchange extends ExchangeStateDecorator<BPACredentialExchange, CredentialExchangeState> {

    @Id
    @AutoPopulated
    private UUID id;

    @DateCreated
    private Instant createdAt;

    @DateUpdated
    private Instant updatedAt;

    @Nullable
    @OneToOne
    private BPASchema schema;

    @Nullable
    @OneToOne
    private BPACredentialDefinition credDef;

    @OneToOne
    private Partner partner;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private CredentialType type = CredentialType.INDY;

    @Nullable
    private String label;

    private String threadId;

    private String credentialExchangeId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private CredentialExchangeRole role = CredentialExchangeRole.ISSUER;

    @Enumerated(EnumType.STRING)
    private CredentialExchangeState state;

    @Nullable
    @TypeDef(type = DataType.JSON)
    private ExchangeStateDecorator.ExchangeStateToTimestamp<CredentialExchangeState> stateToTimestamp;

    @Nullable
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ExchangeVersion exchangeVersion = ExchangeVersion.V1;

    @Nullable
    @TypeDef(type = DataType.JSON)
    private Credential credential; // do not store reference to schema and cred def here

    @Nullable
    @TypeDef(type = DataType.JSON)
    private V1CredentialExchange.CredentialProposalDict.CredentialProposal credentialProposal;

    // revocation - link to issued credential
    /** credential revocation identifier */
    @Nullable
    private String credRevId;
    /** revocation registry identifier */
    @Nullable
    private String revRegId;
    /** if the credential has been revoked */
    @Nullable
    private Boolean revoked;

    @Nullable
    private String errorMsg;

    public Map<String, String> proposalAttributesToMap() {
        if (credentialProposal == null || CollectionUtils.isEmpty(credentialProposal.getAttributes())) {
            return Map.of();
        }
        return credentialProposal.getAttributes()
                .stream()
                .collect(Collectors.toMap(CredentialAttributes::getName, CredentialAttributes::getValue));
    }

    public Map<String, String> credentialAttributesToMap() {
        if (credential == null || CollectionUtils.isEmpty(credential.getAttrs())) {
            return Map.of();
        }
        return credential.getAttrs();
    }

    // extends lombok builder
    public static class BPACredentialExchangeBuilder {
        public BPACredentialExchange.BPACredentialExchangeBuilder pushStateChange(
                @NonNull CredentialExchangeState state, @NonNull Instant ts) {
            this.stateToTimestamp(ExchangeStateDecorator.ExchangeStateToTimestamp.<CredentialExchangeState>builder()
                    .stateToTimestamp(Map.of(state, ts))
                    .build());
            return this;
        }
    }
}
