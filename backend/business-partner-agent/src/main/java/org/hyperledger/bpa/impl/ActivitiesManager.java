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

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.config.ActivityLogConfig;
import org.hyperledger.bpa.controller.api.activity.*;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.Activity;
import org.hyperledger.bpa.repository.ActivityRepository;
import org.hyperledger.bpa.repository.PartnerProofRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor
public class ActivitiesManager {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    PartnerProofRepository proofRepository;

    @Inject
    ActivityLogConfig activityLogConfig;

    @Inject
    ActivityRepository activityRepository;

    @Inject
    Converter converter;

    public List<ActivityItem> getItems(ActivitySearchParameters parameters) {
        List<ActivityItem> results = new ArrayList<>();
        List<String> types = getSearchTypes(parameters);
        List<String> states = getSearchStates(parameters);
        Iterable<Activity> activities = activityRepository.findByTypeIn(types);
        activities.forEach(activity -> {
            // filter based on the appropriate states.
            // we can't add states to the query without losing the auto population of the
            // partner..
            if (states.contains(activity.getState())) {
                PartnerAPI partner = converter.toAPIObject(activity.getPartner());
                // for now, just use the partner as the link..
                ActivityItem item = ActivityItem.builder()
                        .linkId(partner.getId())
                        .partner(partner)
                        .updatedAt(activity.getUpdatedAt().toEpochMilli())
                        .type(getActivityType(activity))
                        .role(getActivityRole(activity))
                        .state(getActivityState(activity))
                        .task(isTask(activity))
                        .build();
                results.add(item);
            }
        });
        return results;
    }

    private ActivityRole getActivityRole(Activity a) {
        switch (ActivityType.valueOf(a.getType())) {
        case CONNECTION_REQUEST:
            return ActivityRole.valueOf(a.getRole());
        case PRESENTATION_EXCHANGE:
        default:
            PresentationExchangeRole per = PresentationExchangeRole.valueOf(a.getRole());
            switch (per) {
            case VERIFIER:
                return ActivityRole.PRESENTATION_EXCHANGE_VERIFIER;
            case PROVER:
            default:
                return ActivityRole.PRESENTATION_EXCHANGE_PROVER;
            }
        }
    }

    private ActivityState getActivityState(Activity a) {
        switch (ActivityType.valueOf(a.getType())) {
        case CONNECTION_REQUEST:
            ConnectionState cs = ConnectionState.valueOf(a.getState());
            if (activityLogConfig.getConnectionStatesCompleted().contains(cs)) {
                return ActivityState.CONNECTION_REQUEST_ACCEPTED;
            } else {
                if (ActivityRole.CONNECTION_REQUEST_SENDER.equals(ActivityRole.valueOf(a.getRole()))) {
                    return ActivityState.CONNECTION_REQUEST_SENT;
                } else {
                    return ActivityState.CONNECTION_REQUEST_RECEIVED;
                }
            }
        case PRESENTATION_EXCHANGE:
        default:
            PresentationExchangeState pes = PresentationExchangeState.valueOf(a.getState());
            switch (pes) {
            case VERIFIED:
            case PRESENTATION_ACKED:
                return ActivityState.PRESENTATION_EXCHANGE_ACCEPTED;
            case REQUEST_SENT:
            case PRESENTATIONS_SENT:
                return ActivityState.PRESENTATION_EXCHANGE_SENT;
            case REQUEST_RECEIVED:
            case PRESENTATION_RECEIVED:
                return ActivityState.PRESENTATION_EXCHANGE_RECEIVED;
            default:
                PresentationExchangeRole per = PresentationExchangeRole.valueOf(a.getRole());
                switch (per) {
                case VERIFIER:
                    return ActivityState.PRESENTATION_EXCHANGE_RECEIVED;
                case PROVER:
                default:
                    return ActivityState.PRESENTATION_EXCHANGE_SENT;
                }
            }
        }
    }

    private ActivityType getActivityType(Activity a) {
        switch (ActivityType.valueOf(a.getType())) {
        case CONNECTION_REQUEST:
            return ActivityType.CONNECTION_REQUEST;
        case PRESENTATION_EXCHANGE:
        default:
            return ActivityType.PRESENTATION_EXCHANGE;
        }
    }

    private boolean isTask(Activity a) {
        switch (ActivityType.valueOf(a.getType())) {
        case CONNECTION_REQUEST:
            return activityLogConfig.getConnectionStatesForTasks().contains(ConnectionState.valueOf(a.getState()));
        case PRESENTATION_EXCHANGE:
            return activityLogConfig.getPresentationExchangeStatesForTasks()
                    .contains(PresentationExchangeState.valueOf(a.getState()));
        default:
            return false;
        }
    }

    private List<String> getSearchTypes(ActivitySearchParameters parameters) {
        if (parameters.getType() != null) {
            return List.of(parameters.getType().toString());
        }
        // TODO: add credential offers in when we support it
        return List.of(ActivityType.PRESENTATION_EXCHANGE.toString(), ActivityType.CONNECTION_REQUEST.toString());
    }

    private List<String> getSearchStates(ActivitySearchParameters parameters) {
        List<String> states = new ArrayList<>();
        if (parameters.getActivity()) {
            states.addAll(activityLogConfig.getConnectionStatesForActivities().stream()
                    .map(v -> v.toString())
                    .collect(Collectors.toList()));
            states.addAll(activityLogConfig.getPresentationExchangeStatesForActivities().stream()
                    .map(v -> v.toString())
                    .collect(Collectors.toList()));
        }
        if (parameters.getTask()) {
            states.addAll(activityLogConfig.getConnectionStatesForTasks().stream()
                    .map(v -> v.toString())
                    .collect(Collectors.toList()));
            states.addAll(activityLogConfig.getPresentationExchangeStatesForTasks().stream()
                    .map(v -> v.toString())
                    .collect(Collectors.toList()));
        }
        return states;
    }

}
