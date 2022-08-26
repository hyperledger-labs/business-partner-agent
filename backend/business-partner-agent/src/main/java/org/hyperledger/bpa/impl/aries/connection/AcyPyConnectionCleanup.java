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
package org.hyperledger.bpa.impl.aries.connection;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.IssueCredentialRecordsFilter;
import org.hyperledger.aries.api.issue_credential_v2.V2IssueCredentialRecordsFilter;
import org.hyperledger.bpa.config.AcaPyConfig;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.PartnerProof;
import org.hyperledger.bpa.persistence.repository.HolderCredExRepository;
import org.hyperledger.bpa.persistence.repository.PartnerProofRepository;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Singleton
public class AcyPyConnectionCleanup {

    private static final int PAGE_SIZE = 50;

    private final Set<CredentialExchangeState> FILTERED_STATES;

    private final AriesClient ac;

    private final PartnerProofRepository partnerProofRepo;

    private final HolderCredExRepository holderCredExRepository;

    @Inject
    public AcyPyConnectionCleanup(
            AriesClient ac,
            PartnerProofRepository partnerProofRepo,
            HolderCredExRepository holderCredExRepository,
            AcaPyConfig acaPyConfig) {
        this.ac = ac;
        this.partnerProofRepo = partnerProofRepo;
        this.holderCredExRepository = holderCredExRepository;

        if (acaPyConfig.getPreserveExchangeRecords()) {
            FILTERED_STATES = Set.of(CredentialExchangeState.DECLINED); // hack as this is no aca-py state
        } else {
            FILTERED_STATES = Set.of(
                    CredentialExchangeState.CREDENTIAL_ACKED, CredentialExchangeState.DONE);
        }
    }

    void deleteConnectionRecord(
            @NonNull String connectionId) {
        try {
            ac.connectionsRemove(connectionId);
        } catch (IOException | AriesException e) {
            log.warn("Could not delete aca-py connection.", e);
        }
    }

    void deletePresentationExchangeRecords(
            @NonNull UUID partnerId) {
        Page<PartnerProof.DeletePartnerProofDTO> partnerProofs = partnerProofRepo.getByPartnerId(
                partnerId, Pageable.from(0, PAGE_SIZE));
        do {
            partnerProofs = deletePresExPage(partnerId, partnerProofs);
        } while (partnerProofs.getNumberOfElements() > 0);
    }

    void deleteCredentialExchangeRecords(
            @NonNull UUID partnerId) {
        // If the flag preserver-exchange-records is set to false, only exchanges that
        // are not acked or done are stored within aca-py, so to prevent lots of 404
        // we filter out those states.
        Page<BPACredentialExchange.DeleteCredentialExchangeDTO> credExchanges = holderCredExRepository
                .findByPartnerIdAndStateNotIn(
                        partnerId,
                        FILTERED_STATES,
                        Pageable.from(0, PAGE_SIZE));
        do {
            credExchanges = deleteCredExPage(partnerId, credExchanges);
        } while (credExchanges.getNumberOfElements() > 0);

    }

    void deleteRemainingCredentialExchangeRecords(
            @NonNull String connectionId) {
        try {
            ac.issueCredentialRecords(IssueCredentialRecordsFilter
                    .builder()
                    .connectionId(connectionId)
                    .build())
                    .ifPresent(records -> records
                            .forEach(record -> deleteCredentialExchangeRecord(record.getCredentialExchangeId(),
                                    ExchangeVersion.V1)));
            ac.issueCredentialV2Records(V2IssueCredentialRecordsFilter
                    .builder()
                    .connectionId(connectionId)
                    .build())
                    .ifPresent(records -> records
                            .forEach(record -> deleteCredentialExchangeRecord(record.getCredExRecord().getCredExId(),
                                    ExchangeVersion.V2)));
        } catch (IOException e) {
            log.error("aca-py not available");
        }
    }

    private Page<PartnerProof.DeletePartnerProofDTO> deletePresExPage(
            @NonNull UUID partnerId,
            @NonNull Page<PartnerProof.DeletePartnerProofDTO> page) {
        page.forEach(p -> {
            try {
                if (p.exchangeIsV1()) {
                    ac.presentProofRecordsRemove(p.getPresentationExchangeId());
                } else if (p.exchangeIsV2()) {
                    ac.presentProofV2RecordsRemove(p.getPresentationExchangeId());
                }
            } catch (IOException | AriesException e) {
                log.error("Could not delete presentation exchange record: {}", p.getPresentationExchangeId(), e);
            }
        });
        return partnerProofRepo.getByPartnerId(partnerId, page.nextPageable());
    }

    private Page<BPACredentialExchange.DeleteCredentialExchangeDTO> deleteCredExPage(
            @NonNull UUID partnerId,
            @NonNull Page<BPACredentialExchange.DeleteCredentialExchangeDTO> page) {
        page.forEach(p -> deleteCredentialExchangeRecord(p.getCredentialExchangeId(), p.getExchangeVersion()));
        return holderCredExRepository.findByPartnerIdAndStateNotIn(partnerId, FILTERED_STATES, page.nextPageable());
    }

    private void deleteCredentialExchangeRecord(
            @NonNull String credentialExchangeId,
            @NonNull ExchangeVersion version) {
        try {
            if (version.isV1()) {
                ac.issueCredentialRecordsRemove(credentialExchangeId);
            } else if (version.isV2()) {
                ac.issueCredentialV2RecordsRemove(credentialExchangeId);
            }
        } catch (IOException | AriesException e) {
            log.error("Could not delete credential exchange record: {}", credentialExchangeId, e);
        }
    }
}
