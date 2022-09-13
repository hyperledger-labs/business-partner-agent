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

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.data.annotation.*;
import io.micronaut.data.model.DataType;
import lombok.*;
import lombok.experimental.Accessors;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.issue_credential_v1.CredExStateTranslator;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecordByFormat;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.controller.api.ExchangeVersionTranslator;
import org.hyperledger.bpa.persistence.model.converter.CredExPayloadConverter;
import org.hyperledger.bpa.persistence.model.converter.ExchangePayload;
import org.hyperledger.bpa.persistence.model.type.ExchangeTypeTranslator;

import javax.persistence.Id;
import javax.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
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
        implements CredExStateTranslator, ExchangeTypeTranslator {

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
    @ManyToOne
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
    @TypeDef(type = DataType.JSON, converter = CredExPayloadConverter.class)
    private ExchangePayload<V1CredentialExchange.CredentialProposalDict.CredentialProposal, V20CredExRecordByFormat.LdProof> credentialProposal;

    @Nullable
    @TypeDef(type = DataType.JSON, converter = CredExPayloadConverter.class)
    private ExchangePayload<V1CredentialExchange.CredentialProposalDict.CredentialProposal, V20CredExRecordByFormat.LdProof> credentialOffer;

    @Nullable
    @TypeDef(type = DataType.JSON, converter = CredExPayloadConverter.class)
    private ExchangePayload<V1CredentialExchange.CredentialProposalDict.CredentialProposal, V20CredExRecordByFormat.LdProof> ldCredential;

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty("credential")
    private Credential indyCredential; // TODO deprecation and use ldCredential?

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
    /** aca-py credential identifier, referent when indy, record_id when json-ld */
    @Nullable
    private String referent;

    public boolean checkIfPublic() {
        return Boolean.TRUE.equals(isPublic);
    }

    public boolean checkIfRevocable() {
        return StringUtils.isNotEmpty(revRegId);
    }

    public boolean checkIfNotRevoked() {
        return revoked == null || !revoked;
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

    public @io.micronaut.core.annotation.NonNull ArrayList<CredentialAttributes> proposalAttributesToMap() {
        if (typeIsJsonLd()) {
            return ldAttributesToMap(credentialProposal != null ? credentialProposal.getJsonLD() : null);
        }
        return indyAttributesToMap(credentialProposal != null ? credentialProposal.getIndy() : null);
    }

    public @io.micronaut.core.annotation.NonNull Map<String, String> offerAttributesToMap() {
        if (typeIsJsonLd()) {
            return ldAttributesToMap(credentialOffer != null ? credentialOffer.getJsonLD() : null);
        }
        return indyAttributesToMap(credentialOffer != null ? credentialOffer.getIndy() : null);
    }

    public @io.micronaut.core.annotation.NonNull Map<String, String> credentialAttributesToMap() {
        if (typeIsJsonLd()) {
            return ldAttributesToMap(ldCredential != null ? ldCredential.getJsonLD() : null);
        }
        // TODO fallback to credential
        if (indyCredential == null || CollectionUtils.isEmpty(indyCredential.getAttrs())) {
            return Map.of();
        }
        return CredentialAttributes.toMap(indyCredential.getAttrs());
    }

    private Map<String, String> ldAttributesToMap(V20CredExRecordByFormat.LdProof ldProof) {
        return ldProof == null ? Map.of() : ldProof.toFlatMap();
    }

    private Map<String, String> indyAttributesToMap(V1CredentialExchange.CredentialProposalDict.CredentialProposal p) {
        if (p == null || CollectionUtils.isEmpty(p.getAttributes())) {
            return Map.of();
        }
        return p.getAttributes()
                .stream()
                .collect(Collectors.toMap(CredentialAttributes::getName, CredentialAttributes::getValue));
    }

    public Map<String, String> attributesByState() {
        if (stateIsProposalReceived()) {
            return proposalAttributesToMap();
        } else if (stateIsOfferReceived()) {
            return offerAttributesToMap();
        } else if (stateIsDone() || stateIsCredentialAcked() || stateIsCredentialIssued()) {
            return credentialAttributesToMap();
        }
        return Map.of();
    }

    public ExchangePayload<V1CredentialExchange.CredentialProposalDict.CredentialProposal, V20CredExRecordByFormat.LdProof> exchangePayloadByState() {
        if (stateIsProposalReceived()) {
            return credentialProposal;
        } else if (stateIsOfferReceived()) {
            return credentialOffer;
        } else if (stateIsDone() || stateIsCredentialIssued()) {
            return ldCredential;
        }
        return null;
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

    @Introspected
    @Data
    @NoArgsConstructor
    public static class DeleteCredentialExchangeDTO implements ExchangeVersionTranslator {
        private String credentialExchangeId;
        private ExchangeVersion exchangeVersion;
    }
}
