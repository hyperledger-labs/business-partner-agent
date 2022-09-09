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
import org.hyperledger.aries.api.present_proof.SendPresentationRequestHelper2;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@Introspected
public class ApproveProofRequest {

    @NotEmpty
    private Map<String, SelectedReferent> selectedReferents;

    @Data
    @NoArgsConstructor
    public static class SelectedReferent {
        private UUID referent;
        private Boolean revealed;
        private String selfAttestedValue;
    }

    public Map<String, SendPresentationRequestHelper2.SelectedMatch.ReferentInfo> toClientAPI() {
        return selectedReferents.entrySet().stream()
                .map(e -> Map.entry(e.getKey(),
                        SendPresentationRequestHelper2.SelectedMatch.ReferentInfo.builder()
                                .referent(e.getValue().getReferent())
                                .revealed(e.getValue().getRevealed())
                                .selfAttestedValue(e.getValue().getSelfAttestedValue())
                                .build()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public List<String> collectReferents() {
        return selectedReferents.values().stream()
                .map(SelectedReferent::getReferent)
                .map(UUID::toString)
                .collect(Collectors.toList());
    }
}
