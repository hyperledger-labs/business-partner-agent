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

import io.micronaut.core.annotation.Nullable;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.config.ActivityLogConfig;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.controller.api.activity.*;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.model.PartnerProof;
import org.hyperledger.bpa.repository.BPACredentialExchangeRepository;
import org.hyperledger.bpa.repository.PartnerProofRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import java.util.*;

@Slf4j
@NoArgsConstructor
public class ActivitiesManager {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    PartnerProofRepository proofRepository;

    @Inject
    BPACredentialExchangeRepository credExRepository;

    @Inject
    ActivityLogConfig activityLogConfig;

    @Inject
    AriesClient ac;

    @Inject
    BPAMessageSource.DefaultMessageSource messageSource;

    @Inject
    PartnerManager partnerManager;

    @Inject
    Converter converter;

    public List<ActivityItem> getActivityListItems(ActivitySearchParameters parameters) {
        List<ActivityItem> results = new ArrayList<>();
        if (parameters.getActivity() == null || parameters.getActivity()) {
            // connection invitations... outgoing.
            results.addAll(getConnectionRequests(parameters.getType(),
                    activityLogConfig.getConnectionStatesForActivities(), false));
            results.addAll(
                    getPresentationExchanges(parameters.getType(),
                            activityLogConfig.getPresentationExchangeStatesForActivities(), false));
        }
        return results;
    }

    public List<ActivityItem> getTaskListItems(ActivitySearchParameters parameters) {
        List<ActivityItem> results = new ArrayList<>();
        if (parameters.getTask() == null || parameters.getTask()) {
            results.addAll(getConnectionRequests(parameters.getType(),
                    activityLogConfig.getConnectionStatesForTasks(), true));
            results.addAll(getPresentationExchanges(parameters.getType(),
                    activityLogConfig.getPresentationExchangeStatesForTasks(), true));
        }
        return results;
    }

    public List<ActivityItem> getItems(ActivitySearchParameters parameters) {
        List<ActivityItem> results = new ArrayList<>();
        results.addAll(getActivityListItems(parameters));
        results.addAll(getTaskListItems(parameters));
        results.sort(Comparator.comparingLong(ActivityItem::getUpdatedAt).reversed());
        return results;
    }

    private List<ActivityItem> getConnectionRequests(@Nullable ActivityType type, List<ConnectionState> states,
            Boolean incoming) {
        List<ActivityItem> results = new ArrayList<>();
        if (type == null || type == ActivityType.CONNECTION_REQUEST) {
            Iterable<Partner> partners = partnerRepo.findByStateIn(states);
            for (Partner p : partners) {
                if (incoming) {
                    // then we are looking for tasks...
                    if (p.getIncoming() != null) {
                        results.add(getConnectionRequestItem(p, true));
                    }
                } else {
                    results.add(getConnectionRequestItem(p, false));
                }
            }
        }
        return results;
    }

    private List<ActivityItem> getPresentationExchanges(ActivityType type, List<PresentationExchangeState> states,
            Boolean task) {
        List<ActivityItem> results = new ArrayList<>();
        if (type == null || type == ActivityType.PRESENTATION_EXCHANGE) {
            Iterable<PartnerProof> proofs = proofRepository.findByStateIn(states);
            for (PartnerProof p : proofs) {
                results.add(getPresentationExchangeItem(p, task));
            }
        }
        return results;
    }

    private ActivityItem getConnectionRequestItem(Partner p, Boolean task) {
        ActivityRole role = (p.getIncoming() == null) ? ActivityRole.CONNECTION_REQUEST_SENDER
                : ActivityRole.CONNECTION_REQUEST_RECIPIENT;
        ActivityType type = ActivityType.CONNECTION_REQUEST;

        ActivityState state;
        switch (p.getState()) {
        case ACTIVE:
        case RESPONSE:
            state = ActivityState.CONNECTION_REQUEST_ACCEPTED;
            break;
        default:
            switch (role) {
            case CONNECTION_REQUEST_SENDER:
                state = ActivityState.CONNECTION_REQUEST_SENT;
                break;
            default:
                state = ActivityState.CONNECTION_REQUEST_RECEIVED;
            }
        }

        Long updatedAt = p.getUpdatedAt().toEpochMilli();
        UUID linkId = p.getId();
        PartnerAPI apiPartner = getPartner(linkId);
        return ActivityItem.builder()
                .role(role)
                .state(state)
                .type(type)
                .partner(apiPartner)
                .linkId(linkId.toString())
                .task(task)
                .updatedAt(updatedAt)
                .build();
    }

    private ActivityItem getPresentationExchangeItem(PartnerProof p, Boolean task) {
        ActivityRole role = p.getRole() == PresentationExchangeRole.PROVER ? ActivityRole.PRESENTATION_EXCHANGE_PROVER
                : ActivityRole.PRESENTATION_EXCHANGE_VERIFIER;
        ActivityType type = ActivityType.PRESENTATION_EXCHANGE;

        ActivityState state;
        switch (p.getState()) {
        case VERIFIED:
        case PRESENTATION_ACKED:
            state = ActivityState.PRESENTATION_EXCHANGE_ACCEPTED;
            break;
        case REQUEST_SENT:
        case PRESENTATIONS_SENT:
            state = ActivityState.PRESENTATION_EXCHANGE_SENT;
            break;
        case REQUEST_RECEIVED:
        case PRESENTATION_RECEIVED:
            state = ActivityState.PRESENTATION_EXCHANGE_RECEIVED;
            break;
        default:
            switch (role) {
            case PRESENTATION_EXCHANGE_PROVER:
                state = ActivityState.PRESENTATION_EXCHANGE_SENT;
                break;
            default:
                state = ActivityState.PRESENTATION_EXCHANGE_RECEIVED;
            }
        }

        UUID linkId = p.getPartnerId();
        PartnerAPI apiPartner = getPartner(linkId);
        Long updatedAt = (p.getIssuedAt() != null) ? p.getIssuedAt().toEpochMilli() : p.getCreatedAt().toEpochMilli();

        return ActivityItem.builder()
                .role(role)
                .state(state)
                .type(type)
                .partner(apiPartner)
                .linkId(linkId.toString())
                .task(task)
                .updatedAt(updatedAt)
                .build();
    }

    @Nullable
    private PartnerAPI getPartner(UUID linkId) {
        PartnerAPI apiPartner = null;
        Optional<Partner> dbPartner = partnerRepo.findById(linkId);
        if (dbPartner.isPresent()) {
            apiPartner = converter.toAPIObject(dbPartner.get());
        }
        return apiPartner;
    }

}
