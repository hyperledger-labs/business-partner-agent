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

import com.github.jknack.handlebars.internal.lang3.StringUtils;
import io.micronaut.core.annotation.Nullable;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.bpa.controller.api.activity.*;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.model.PartnerProof;
import org.hyperledger.bpa.repository.BPACredentialExchangeRepository;
import org.hyperledger.bpa.repository.PartnerProofRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import java.util.*;

public class ActivitiesManager {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    PartnerProofRepository proofRepository;

    @Inject
    BPACredentialExchangeRepository credExRepository;

    static List<ConnectionState> connectionStates(ConnectionState... states) {
        List<ConnectionState> results = new ArrayList<>();
        for (ConnectionState state : states)
            results.add(state);
        return results;
    }

    static List<ConnectionState> connectionStatesForActivities() {
        return connectionStates(ConnectionState.REQUEST, ConnectionState.INVITATION, ConnectionState.ACTIVE,
                ConnectionState.INVITATION.RESPONSE);
    }

    static List<ConnectionState> connectionStatesForTasks() {
        return connectionStates(ConnectionState.REQUEST);
    }

    static List<ConnectionState> connectionStatesCompleted() {
        return connectionStates(ConnectionState.ACTIVE, ConnectionState.INVITATION.RESPONSE);
    }

    static List<PresentationExchangeState> presentationExchangeStates(PresentationExchangeState... states) {
        List<PresentationExchangeState> results = new ArrayList<>();
        for (PresentationExchangeState state : states)
            results.add(state);
        return results;
    }

    static List<PresentationExchangeState> presentationExchangeStatesForActivities() {
        return presentationExchangeStates(PresentationExchangeState.REQUEST_RECEIVED,
                PresentationExchangeState.REQUEST_SENT,
                PresentationExchangeState.VERIFIED,
                PresentationExchangeState.PRESENTATION_ACKED);
    }

    static List<PresentationExchangeState> presentationExchangeStatesForTasks() {
        return presentationExchangeStates(PresentationExchangeState.REQUEST_RECEIVED);
    }

    static List<PresentationExchangeState> presentationExchangeStatesCompleted() {
        return presentationExchangeStates(PresentationExchangeState.VERIFIED,
                PresentationExchangeState.PRESENTATION_ACKED);
    }

    static List<CredentialExchangeState> credentialExchangeStates(CredentialExchangeState... states) {
        List<CredentialExchangeState> results = new ArrayList<>();
        for (CredentialExchangeState state : states)
            results.add(state);
        return results;
    }

    static List<CredentialExchangeState> credentialExchangeStatesForActivities() {
        return credentialExchangeStates(CredentialExchangeState.OFFER_SENT,
                CredentialExchangeState.REQUEST_SENT,
                CredentialExchangeState.PROPOSAL_SENT);
    }

    static List<CredentialExchangeState> credentialExchangeStatesForTasks() {
        return credentialExchangeStates(CredentialExchangeState.OFFER_RECEIVED,
                CredentialExchangeState.REQUEST_RECEIVED,
                CredentialExchangeState.PROPOSAL_RECEIVED);
    }

    public List<ActivityItem> getActivityListItems(ActivitySearchParameters parameters) {
        List<ActivityItem> results = new ArrayList<>();
        if (parameters.getActivity() == null || parameters.getActivity()) {
            // connection invitations... outgoing.
            results.addAll(getConnectionInvitations(parameters.getType(), connectionStatesForActivities(), false));
            results.addAll(
                    getPresentationExchanges(parameters.getType(), presentationExchangeStatesForActivities(), false));
        }
        return results;
    }

    public List<ActivityItem> getTaskListItems(ActivitySearchParameters parameters) {
        List<ActivityItem> results = new ArrayList<>();
        if (parameters.getTask() == null || parameters.getTask()) {
            // connection invitations... incoming.
            results.addAll(getConnectionInvitations(parameters.getType(), connectionStatesForTasks(), true));
            results.addAll(getPresentationExchanges(parameters.getType(), presentationExchangeStatesForTasks(), true));
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

    private List<ActivityItem> getConnectionInvitations(@Nullable ActivityType type, List<ConnectionState> states,
            Boolean incoming) {
        List<ActivityItem> results = new ArrayList<>();
        if (type == null || type == ActivityType.CONNECTION_INVITATION) {
            Iterable<Partner> partners = partnerRepo.findByStateIn(states);
            for (Partner p : partners) {
                if (incoming) {
                    // then we are looking for tasks...
                    if (p.getIncoming() != null) {
                        results.add(getConnectionInvitationItem(p, true));
                    }
                } else {
                    results.add(getConnectionInvitationItem(p, false));
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

    private ActivityItem getConnectionInvitationItem(Partner p, Boolean task) {
        ActivityRole role = (p.getIncoming() == null) ? ActivityRole.CONNECTION_INVITATION_SENDER
                : ActivityRole.CONNECTION_INVITATION_RECIPIENT;
        ActivityType type = ActivityType.CONNECTION_INVITATION;

        ActivityState state;
        switch (p.getState()) {
        case ACTIVE:
        case RESPONSE:
            state = ActivityState.CONNECTION_REQUEST_ACCEPTED;
            break;
        default:
            switch (role) {
            case CONNECTION_INVITATION_SENDER:
                state = ActivityState.CONNECTION_REQUEST_SENT;
                break;
            default:
                state = ActivityState.CONNECTION_REQUEST_RECEIVED;
            }
        }

        String alias = getConnectionAlias(p);
        Long updatedAt = p.getUpdatedAt().toEpochMilli();
        String linkId = p.getId().toString();
        return ActivityItem.builder()
                .role(role)
                .state(state)
                .type(type)
                .connectionAlias(alias)
                .linkId(linkId)
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
        String linkId = p.getId().toString();
        String alias = "";
        Optional<Partner> partner = partnerRepo.findById(p.getPartnerId());
        if (partner.isPresent()) {
            alias = getConnectionAlias(partner.get());
            // TODO: remove this when we have the proof request details screen
            linkId = partner.get().getId().toString();
        }
        Long updatedAt = (p.getIssuedAt() != null) ? p.getIssuedAt().toEpochMilli() : p.getCreatedAt().toEpochMilli();

        return ActivityItem.builder()
                .role(role)
                .state(state)
                .type(type)
                .connectionAlias(alias)
                .linkId(linkId)
                .task(task)
                .updatedAt(updatedAt)
                .build();
    }

    private String getConnectionAlias(Partner partner) {
        return StringUtils.isNotEmpty(partner.getAlias()) ? partner.getAlias()
                : StringUtils.isNotEmpty(partner.getLabel()) ? partner.getLabel() : partner.getDid();
    }

}
