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

import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.NonNull;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.bpa.config.ActivityLogConfig;
import org.hyperledger.bpa.controller.api.activity.*;
import org.hyperledger.bpa.impl.notification.TaskAddedEvent;
import org.hyperledger.bpa.impl.notification.TaskCompletedEvent;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.Activity;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.model.PartnerProof;
import org.hyperledger.bpa.repository.ActivityRepository;
import org.hyperledger.bpa.repository.PartnerProofRepository;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor
public class ActivityManager {

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

    @Inject
    ApplicationEventPublisher eventPublisher;

    public List<ActivityItem> getItems(ActivitySearchParameters parameters) {
        List<Activity> activities = new ArrayList<>();

        if (parameters.getActivity() && parameters.getTask()) {
            if (parameters.getType() != null) {
                activities = activityRepository.findByTypeOrderByUpdatedAt(parameters.getType());
            } else {
                activities = activityRepository.listOrderByUpdatedAtDesc();
            }
        } else if (parameters.getTask()) {
            if (parameters.getType() != null) {
                activities = activityRepository
                        .findByTypeAndCompletedFalseOrderByUpdatedAtDesc(parameters.getType());
            } else {
                activities = activityRepository.findByCompletedFalseOrderByUpdatedAtDesc();
            }
        } else {
            if (parameters.getType() != null) {
                activities = activityRepository
                        .findByTypeAndCompletedTrueOrderByUpdatedAtDesc(parameters.getType());
            } else {
                activities = activityRepository.findByCompletedTrueOrderByUpdatedAtDesc();
            }
        }

        return activities.stream().map(this::convert).collect(Collectors.toList());
    }

    public void addPartnerRequestReceivedTask(@NonNull Partner partner) {
        // in case event is fired multiple times
        Optional<Activity> existing = activityRepository.findByLinkIdAndTypeAndRole(partner.getId(),
                ActivityType.CONNECTION_REQUEST,
                ActivityRole.CONNECTION_REQUEST_RECIPIENT);
        if (existing.isEmpty()) {
            Activity a = Activity.builder()
                    .linkId(partner.getId())
                    .partner(partner)
                    .type(ActivityType.CONNECTION_REQUEST)
                    .role(ActivityRole.CONNECTION_REQUEST_RECIPIENT)
                    .state(ActivityState.CONNECTION_REQUEST_RECEIVED)
                    .completed(false)
                    .build();
            activityRepository.save(a);
            eventPublisher.publishEventAsync(TaskAddedEvent.builder().activity(a).build());
        }
    }

    public void completePartnerRequestTask(@NonNull Partner partner) {
        activityRepository.findByLinkIdAndTypeAndRole(partner.getId(),
                ActivityType.CONNECTION_REQUEST,
                ActivityRole.CONNECTION_REQUEST_RECIPIENT).ifPresentOrElse(activity -> {
                    // set to completed and mark accepted
                    activity.setState(ActivityState.CONNECTION_REQUEST_ACCEPTED);
                    activity.setCompleted(true);
                    activityRepository.update(activity);
                    eventPublisher.publishEventAsync(TaskCompletedEvent.builder().activity(activity).build());
                }, () -> {
                    // add in a completed activity
                    Activity a = Activity.builder()
                            .linkId(partner.getId())
                            .partner(partner)
                            .type(ActivityType.CONNECTION_REQUEST)
                            .role(ActivityRole.CONNECTION_REQUEST_RECIPIENT)
                            .state(ActivityState.CONNECTION_REQUEST_ACCEPTED)
                            .completed(true)
                            .build();
                    activityRepository.save(a);
                });
    }

    public void deletePartnerActivities(@NonNull Partner partner) {
        activityRepository.deleteByPartnerId(partner.getId());
    }

    public void addPartnerAddedActivity(@NonNull Partner partner) {
        Optional<Activity> existing = activityRepository.findByLinkIdAndTypeAndRole(partner.getId(),
                ActivityType.CONNECTION_REQUEST,
                ActivityRole.CONNECTION_REQUEST_SENDER);
        if (existing.isEmpty()) {
            Activity a = Activity.builder()
                    .linkId(partner.getId())
                    .partner(partner)
                    .type(ActivityType.CONNECTION_REQUEST)
                    .role(ActivityRole.CONNECTION_REQUEST_SENDER)
                    .state(ActivityState.CONNECTION_REQUEST_SENT)
                    .completed(true)
                    .build();
            activityRepository.save(a);
        }
    }

    public void addPartnerAcceptedActivity(@NonNull Partner partner) {
        activityRepository.findByLinkIdAndTypeAndRole(partner.getId(),
                ActivityType.CONNECTION_REQUEST,
                ActivityRole.CONNECTION_REQUEST_SENDER).ifPresentOrElse(activity -> {
                    activity.setState(ActivityState.CONNECTION_REQUEST_ACCEPTED);
                    activity.setCompleted(true);
                    activityRepository.update(activity);
                }, () -> {
                    // add in a completed activity
                    Activity a = Activity.builder()
                            .linkId(partner.getId())
                            .partner(partner)
                            .type(ActivityType.CONNECTION_REQUEST)
                            .role(ActivityRole.CONNECTION_REQUEST_SENDER)
                            .state(ActivityState.CONNECTION_REQUEST_ACCEPTED)
                            .completed(true)
                            .build();
                    activityRepository.save(a);
                });
    }

    public void addPresentationExchangeTask(@NonNull PartnerProof partnerProof) {
        partnerRepo.findById(partnerProof.getPartnerId()).ifPresent(partner -> {
            // in case event is fired multiple times, see if already exists.
            ActivityRole role = getPresentationExchangeRole(partnerProof);
            ActivityState state = getPresentationExchangeState(partnerProof);

            Optional<Activity> existing = activityRepository.findByLinkIdAndTypeAndRole(partnerProof.getId(),
                    ActivityType.PRESENTATION_EXCHANGE,
                    role);

            if (existing.isEmpty()) {
                Activity a = Activity.builder()
                        .linkId(partnerProof.getId())
                        .partner(partner)
                        .type(ActivityType.PRESENTATION_EXCHANGE)
                        .role(role)
                        .state(state)
                        .completed(ActivityState.PRESENTATION_EXCHANGE_SENT.equals(state))
                        .build();
                activityRepository.save(a);

                if (!a.isCompleted()) {
                    // this looks like we created a task!
                    eventPublisher.publishEventAsync(TaskAddedEvent.builder().activity(a).build());
                }
            }
        });
    }

    public void completePresentationExchangeTask(@NonNull PartnerProof partnerProof) {
        partnerRepo.findById(partnerProof.getPartnerId()).ifPresent(partner -> {
            // in case event is fired multiple times, see if already exists.
            ActivityRole role = getPresentationExchangeRole(partnerProof);
            ActivityState state = ActivityState.PRESENTATION_EXCHANGE_ACCEPTED;

            activityRepository.findByLinkIdAndTypeAndRole(partnerProof.getId(),
                    ActivityType.PRESENTATION_EXCHANGE,
                    role).ifPresentOrElse(activity -> {
                        // set to completed and mark accepted
                        activity.setState(state);
                        activity.setCompleted(true);
                        activityRepository.update(activity);

                        eventPublisher.publishEventAsync(TaskCompletedEvent.builder().activity(activity).build());
                    }, () -> {
                        // add in a completed activity
                        Activity a = Activity.builder()
                                .linkId(partnerProof.getId())
                                .partner(partner)
                                .type(ActivityType.PRESENTATION_EXCHANGE)
                                .role(role)
                                .state(state)
                                .completed(true)
                                .build();
                        activityRepository.save(a);
                    });
        });
    }

    public void declinePresentationExchangeTask(@NonNull PartnerProof partnerProof) {
        partnerRepo.findById(partnerProof.getPartnerId()).ifPresent(partner -> {
            // in case event is fired multiple times, see if already exists.
            ActivityRole role = getPresentationExchangeRole(partnerProof);

            activityRepository.findByLinkIdAndTypeAndRole(partnerProof.getId(),
                    ActivityType.PRESENTATION_EXCHANGE,
                    role).ifPresent(activity -> {
                        activity.setState(ActivityState.PRESENTATION_EXCHANGE_DECLINED);
                        activity.setCompleted(true);
                        activityRepository.update(activity);
                        eventPublisher.publishEventAsync(TaskCompletedEvent.builder().activity(activity).build());
                    });
        });
    }

    public void deletePresentationExchangeTask(@NonNull PartnerProof partnerProof) {
        ActivityRole role = getPresentationExchangeRole(partnerProof);
        activityRepository.findByLinkIdAndTypeAndRole(partnerProof.getId(),
                ActivityType.PRESENTATION_EXCHANGE,
                role).ifPresent(activity -> {
                    activityRepository.delete(activity);
                    eventPublisher.publishEventAsync(TaskCompletedEvent.builder().activity(activity).build());
                });
    }

    private ActivityItem convert(Activity activity) {
        return ActivityItem.builder()
                .id(activity.getId().toString())
                .linkId(activity.getLinkId().toString())
                .partner(converter.toAPIObject(activity.getPartner()))
                .role(activity.getRole())
                .state(activity.getState())
                .type(activity.getType())
                .updatedAt(activity.getUpdatedAt().toEpochMilli())
                .completed(activity.isCompleted())
                .build();
    }

    private ActivityState getPresentationExchangeState(PartnerProof partnerProof) {
        switch (partnerProof.getState()) {
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
            switch (partnerProof.getRole()) {
            case VERIFIER:
                return ActivityState.PRESENTATION_EXCHANGE_RECEIVED;
            case PROVER:
            default:
                return ActivityState.PRESENTATION_EXCHANGE_SENT;
            }
        }
    }

    @NotNull
    private ActivityRole getPresentationExchangeRole(@NonNull PartnerProof partnerProof) {
        return PresentationExchangeRole.PROVER.equals(partnerProof.getRole())
                ? ActivityRole.PRESENTATION_EXCHANGE_PROVER
                : ActivityRole.PRESENTATION_EXCHANGE_VERIFIER;
    }

}
