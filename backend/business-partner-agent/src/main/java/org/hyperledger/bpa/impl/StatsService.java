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
package org.hyperledger.bpa.impl;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.controller.api.stats.BPAStats;
import org.hyperledger.bpa.controller.api.stats.DashboardCounts;
import org.hyperledger.bpa.impl.activity.Identity;
import org.hyperledger.bpa.repository.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Singleton
public class StatsService {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    HolderCredExRepository holderCredExRepo;

    @Inject
    MyDocumentRepository docRepo;

    @Inject
    Identity identity;

    @Inject
    ActivityRepository activityRepository;

    @Inject
    PartnerProofRepository proofRepository;

    public BPAStats collectStats() {
        DashboardCounts totals = DashboardCounts
                .builder()
                .credentialsSent(holderCredExRepo.countByRoleEqualsAndStateEquals(
                        CredentialExchangeRole.ISSUER,
                        CredentialExchangeState.CREDENTIAL_ACKED))
                .credentialsReceived(holderCredExRepo.countByRoleEqualsAndStateEquals(
                        CredentialExchangeRole.HOLDER, CredentialExchangeState.CREDENTIAL_ACKED))
                .tasks(activityRepository.countByCompletedFalse())
                .partners(partnerRepo.countByStateNotEquals(ConnectionState.INVITATION))
                .presentationRequestsSent(proofRepository.countByStateEquals(PresentationExchangeState.REQUEST_SENT))
                .presentationRequestsReceived(
                        proofRepository.countByStateEquals(PresentationExchangeState.PRESENTATION_RECEIVED))
                .build();

        // for now, let's just get new data created today.
        // we could maybe pass in a date from the ux for different filter/period (last
        // week, last month, ???)
        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
        DashboardCounts periodTotals = DashboardCounts
                .builder()
                .credentialsSent(holderCredExRepo.countByRoleEqualsAndStateEqualsAndCreatedAtAfter(
                        CredentialExchangeRole.ISSUER, CredentialExchangeState.CREDENTIAL_ACKED, yesterday))
                .credentialsReceived(holderCredExRepo.countByRoleEqualsAndStateEqualsAndCreatedAtAfter(
                        CredentialExchangeRole.HOLDER, CredentialExchangeState.CREDENTIAL_ACKED, yesterday))
                .tasks(activityRepository.countByCompletedFalseAndCreatedAtAfter(yesterday))
                .partners(partnerRepo.countByStateNotEqualsAndCreatedAtAfter(ConnectionState.INVITATION, yesterday))
                .presentationRequestsSent(proofRepository
                        .countByStateEqualsAndCreatedAtAfter(PresentationExchangeState.REQUEST_SENT, yesterday))
                .presentationRequestsReceived(proofRepository.countByStateEqualsAndCreatedAtAfter(
                        PresentationExchangeState.PRESENTATION_RECEIVED, yesterday))
                .build();

        return BPAStats
                .builder()
                .did(identity.getMyPublicDid())
                .profile(docRepo
                        .existsByTypeEqualsAndIsPublicTrue(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL))
                .totals(totals)
                .periodTotals(periodTotals)
                .build();
    }
}
