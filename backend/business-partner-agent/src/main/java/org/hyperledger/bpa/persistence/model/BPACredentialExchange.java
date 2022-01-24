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
package org.hyperledger.bpa.persistence.model;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import lombok.*;
import lombok.experimental.Accessors;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.issue_credential_v1.*;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.bpa.persistence.model.type.CredentialTypeTranslator;

import javax.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stores credential exchanges, either holder or issuer. An exchange is NOT to
 * be confused with the verifiable credential (VC) that is part of the public
 * profile. When an aries credential is made public it will become a VC as part
 * of the public profile.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
@Accessors(chain = true)
@Entity
@Table(name = "bpa_credential_exchange")
public class BPACredentialExchange
        extends StateChangeDecorator<BPACredentialExchange, CredentialExchangeState>
        implements CredExStateTranslator, CredentialTypeTranslator {

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

    @Nullable
    @OneToOne
    private Partner partner;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private CredentialType type = CredentialType.INDY;

    @Nullable
    private String label;

    /** aca-py thread id */
    private String threadId;

    /** temporary credential exchange identifier */
    private String credentialExchangeId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private CredentialExchangeRole role = CredentialExchangeRole.ISSUER;

    @Nullable
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ExchangeVersion exchangeVersion = ExchangeVersion.V1;

    @Enumerated(EnumType.STRING)
    private CredentialExchangeState state;

    @Nullable
    @TypeDef(type = DataType.JSON)
    private StateToTimestamp<CredentialExchangeState> stateToTimestamp;

    @Nullable
    @TypeDef(type = DataType.JSON)
    private V1CredentialExchange.CredentialProposalDict.CredentialProposal credentialProposal;

    @Nullable
    @TypeDef(type = DataType.JSON)
    private V1CredentialExchange.CredentialProposalDict.CredentialProposal credentialOffer;

    @Nullable
    @TypeDef(type = DataType.JSON)
    private Credential credential;

    @Nullable
    private String errorMsg;

    // revocation - links to issued credential
    /** credential revocation identifier */
    @Nullable
    private String credRevId;
    /** revocation registry identifier */
    @Nullable
    private String revRegId;
    /** if the credential has been revoked */
    @Nullable
    private Boolean revoked;

    // holder only
    @Nullable
    private Boolean isPublic;
    @Nullable
    private String issuer;
    /** aca-py credential identifier */
    @Nullable
    private String referent;

    public boolean checkIfPublic() {
        return isPublic != null && isPublic;
    }

    public Instant calculateIssuedAt() {
        return stateToTimestamp != null && stateToTimestamp.getStateToTimestamp() != null
                ? stateToTimestamp.getStateToTimestamp().entrySet()
                        .stream()
                        .filter(e -> CredentialExchangeState.CREDENTIAL_ACKED.equals(e.getKey())
                                || CredentialExchangeState.DONE.equals(e.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null)
                : null;
    }

    public @io.micronaut.core.annotation.NonNull Map<String, String> proposalAttributesToMap() {
        return attributesToMap(credentialProposal);
    }

    public @io.micronaut.core.annotation.NonNull Map<String, String> offerAttributesToMap() {
        return attributesToMap(credentialOffer);
    }

    private Map<String, String> attributesToMap(V1CredentialExchange.CredentialProposalDict.CredentialProposal p) {
        if (p == null || CollectionUtils.isEmpty(p.getAttributes())) {
            return Map.of();
        }
        return p.getAttributes()
                .stream()
                .collect(Collectors.toMap(CredentialAttributes::getName, CredentialAttributes::getValue));
    }

    public @io.micronaut.core.annotation.NonNull Map<String, String> credentialAttributesToMap() {
        if (credential == null || CollectionUtils.isEmpty(credential.getAttrs())) {
            return Map.of();
        }
        return credential.getAttrs();
    }

    // extends lombok builder
    public static class BPACredentialExchangeBuilder {
        public BPACredentialExchange.BPACredentialExchangeBuilder pushStateChange(
                @NonNull CredentialExchangeState state, @NonNull Instant ts) {
            this.stateToTimestamp(StateToTimestamp.<CredentialExchangeState>builder()
                    .stateToTimestamp(Map.of(state, ts))
                    .build());
            return this;
        }
    }
}
