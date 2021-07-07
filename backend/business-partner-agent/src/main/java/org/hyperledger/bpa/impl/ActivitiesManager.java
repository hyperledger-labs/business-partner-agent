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
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.bpa.controller.api.activity.ActivityItem;
import org.hyperledger.bpa.controller.api.activity.ActivitySearchParameters;
import org.hyperledger.bpa.controller.api.activity.ActivityState;
import org.hyperledger.bpa.controller.api.activity.ActivityType;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.BPACredentialExchangeRepository;
import org.hyperledger.bpa.repository.PartnerProofRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    static List<PresentationExchangeState> presentationExchangeStates(PresentationExchangeState... states) {
        List<PresentationExchangeState> results = new ArrayList<>();
        for (PresentationExchangeState state : states)
            results.add(state);
        return results;
    }

    static List<PresentationExchangeState> presentationExchangeStatesForActivities() {
        return presentationExchangeStates(PresentationExchangeState.REQUEST_SENT,
                PresentationExchangeState.PROPOSAL_SENT);
    }

    static List<PresentationExchangeState> presentationExchangeStatesForTasks() {
        return presentationExchangeStates(PresentationExchangeState.REQUEST_RECEIVED,
                PresentationExchangeState.PROPOSAL_RECEIVED);
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
        }
        return results;
    }

    public List<ActivityItem> getTaskListItems(ActivitySearchParameters parameters) {
        List<ActivityItem> results = new ArrayList<>();
        if (parameters.getTask() == null || parameters.getTask()) {
            // connection invitations... incoming.
            results.addAll(getConnectionInvitations(parameters.getType(), connectionStatesForTasks(), true));
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
                    // we want outgoing, ones we sent...
                    if (p.getIncoming() == null) {
                        results.add(getConnectionInvitationItem(p, false));
                    }
                }
            }
        }
        return results;
    }

    private ActivityItem getConnectionInvitationItem(Partner p, Boolean task) {
        ActivityType type = ActivityType.CONNECTION_INVITATION;
        ActivityState state = ActivityState.CONNECTION_REQUEST_RECEIVED;
        if (p.getIncoming() == null) {
            // we sent the invitation...
            switch (p.getState()) {
            case ACTIVE:
            case RESPONSE:
                state = ActivityState.CONNECTION_REQUEST_ACCEPTED;
                break;
            default:
                state = ActivityState.CONNECTION_REQUEST_SENT;
            }
        }
        String alias = StringUtils.isNotEmpty(p.getAlias()) ? p.getAlias()
                : StringUtils.isNotEmpty(p.getLabel()) ? p.getLabel() : p.getDid();
        Long updatedAt = p.getUpdatedAt().toEpochMilli();
        String linkId = p.getId().toString();
        return ActivityItem.builder()
                .state(state)
                .type(type)
                .connectionAlias(alias)
                .linkId(linkId)
                .task(task)
                .updatedAt(updatedAt)
                .build();
    }

}
