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
import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import lombok.*;
import lombok.experimental.Accessors;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;

import javax.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Aries proof that I received from a partner (aka connection).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Accessors(chain = true)
public class PartnerProof extends StateChangeDecorator<PartnerProof, PresentationExchangeState> {

    @Id
    @AutoPopulated
    private UUID id;

    private UUID partnerId;

    @DateCreated
    private Instant createdAt;

    @DateUpdated
    private Instant updatedAt;

    @Nullable
    private Boolean valid;

    @Nullable
    private String threadId;

    private String presentationExchangeId;

    @Nullable
    @Enumerated(EnumType.STRING)
    private PresentationExchangeState state;

    @Nullable
    @Enumerated(EnumType.STRING)
    private PresentationExchangeRole role;

    @Nullable
    @Enumerated(EnumType.STRING)
    private ExchangeVersion exchangeVersion;

    @Nullable
    private String problemReport;

    @Nullable
    @TypeDef(type = DataType.JSON)
    private Map<String, Object> proof;

    @TypeDef(type = DataType.JSON)
    private PresentProofRequest.ProofRequest proofRequest;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    private BPAProofTemplate proofTemplate;

    @TypeDef(type = DataType.JSON)
    private StateToTimestamp<PresentationExchangeState> stateToTimestamp;

    // extends lombok builder
    public static class PartnerProofBuilder {
        public PartnerProofBuilder pushStateChange(@NonNull PresentationExchangeState state, @NonNull Instant ts) {
            this.stateToTimestamp(StateToTimestamp.<PresentationExchangeState>builder()
                    .stateToTimestamp(Map.of(state, ts))
                    .build());
            return this;
        }
    }

    public boolean isV1Exchange() {
        return exchangeVersion != null && exchangeVersion.isV1();
    }
}
