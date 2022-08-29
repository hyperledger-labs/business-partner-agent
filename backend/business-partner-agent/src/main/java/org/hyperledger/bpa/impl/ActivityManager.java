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
package org.hyperledger.bpa.impl;

import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.bpa.api.aries.AriesCredential;
import org.hyperledger.bpa.api.notification.ActivityNotificationEvent;
import org.hyperledger.bpa.api.notification.TaskAddedEvent;
import org.hyperledger.bpa.api.notification.TaskCompletedEvent;
import org.hyperledger.bpa.controller.api.activity.*;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.persistence.model.Activity;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.PartnerProof;
import org.hyperledger.bpa.persistence.repository.ActivityRepository;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;

import java.util.Optional;

@Slf4j
@NoArgsConstructor
public class ActivityManager {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    ActivityRepository activityRepository;

    @Inject
    Converter converter;

    @Inject
    ApplicationEventPublisher eventPublisher;

    public Page<ActivityItem> getItems(
            @NonNull ActivitySearchParameters parameters,
            @NonNull Pageable pageable) {
        Page<Activity> activities;

        if (parameters.hasActivity() && parameters.hasTask()) {
            if (parameters.getType() != null) {
                activities = activityRepository.findByType(parameters.getType(), pageable);
            } else {
                activities = activityRepository.list(pageable);
            }
        } else if (parameters.hasTask()) {
            if (parameters.getType() != null) {
                activities = activityRepository
                        .findByTypeAndCompletedFalse(parameters.getType(), pageable);
            } else {
                activities = activityRepository.findByCompletedFalse(pageable);
            }
        } else {
            if (parameters.getType() != null) {
                activities = activityRepository
                        .findByTypeAndCompletedTrue(parameters.getType(), pageable);
            } else {
                activities = activityRepository.findByCompletedTrue(pageable);
            }
        }

        return activities.map(this::convert);
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

    public void completeCredentialOfferedTask(@NonNull AriesCredential credential) {
        partnerRepo.findByConnectionId(credential.getConnectionId()).ifPresent(partner -> activityRepository
                .findByLinkIdAndTypeAndRole(credential.getId(),
                        ActivityType.CREDENTIAL_EXCHANGE,
                        ActivityRole.CREDENTIAL_EXCHANGE_HOLDER)
                .ifPresentOrElse(activity -> {
                    activity.setState(ActivityState.CREDENTIAL_EXCHANGE_ACCEPTED);
                    activity.setCompleted(true);
                    activityRepository.update(activity);
                    eventPublisher.publishEventAsync(TaskCompletedEvent.builder().activity(activity).build());
                },
                        () -> {
                            Activity a = Activity.builder()
                                    .linkId(credential.getId())
                                    .partner(partner)
                                    .type(ActivityType.CREDENTIAL_EXCHANGE)
                                    .role(ActivityRole.CREDENTIAL_EXCHANGE_HOLDER)
                                    .state(ActivityState.CREDENTIAL_EXCHANGE_ACCEPTED)
                                    .completed(true)
                                    .build();
                            activityRepository.save(a);
                            eventPublisher
                                    .publishEventAsync(ActivityNotificationEvent.builder().activity(a).build());
                        }));
    }

    public void addCredentialOfferedTask(@NonNull AriesCredential credential) {
        partnerRepo.findByConnectionId(credential.getConnectionId()).ifPresent(partner -> {
            Optional<Activity> existing = activityRepository.findByLinkIdAndTypeAndRole(credential.getId(),
                    ActivityType.CREDENTIAL_EXCHANGE,
                    ActivityRole.CREDENTIAL_EXCHANGE_HOLDER);
            if (existing.isEmpty()) {
                Activity a = Activity.builder()
                        .linkId(credential.getId())
                        .partner(partner)
                        .type(ActivityType.CREDENTIAL_EXCHANGE)
                        .role(ActivityRole.CREDENTIAL_EXCHANGE_HOLDER)
                        .state(ActivityState.CREDENTIAL_EXCHANGE_RECEIVED)
                        .completed(false)
                        .build();
                activityRepository.save(a);
                eventPublisher.publishEventAsync(TaskAddedEvent.builder().activity(a).build());
            }
        });
    }

    public void addCredentialIssuedActivity(@NonNull AriesCredential credential) {
        partnerRepo.findByConnectionId(credential.getConnectionId()).ifPresent(partner -> activityRepository
                .findByLinkIdAndTypeAndRole(credential.getId(),
                        ActivityType.CREDENTIAL_EXCHANGE,
                        ActivityRole.CREDENTIAL_EXCHANGE_ISSUER)
                .ifPresentOrElse(activity -> {
                    activity.setState(ActivityState.CREDENTIAL_EXCHANGE_SENT);
                    activity.setCompleted(true);
                    activityRepository.update(activity);
                },
                        () -> {
                            Activity a = Activity.builder()
                                    .linkId(credential.getId())
                                    .partner(partner)
                                    .type(ActivityType.CREDENTIAL_EXCHANGE)
                                    .role(ActivityRole.CREDENTIAL_EXCHANGE_ISSUER)
                                    .state(ActivityState.CREDENTIAL_EXCHANGE_SENT)
                                    .completed(true)
                                    .build();
                            activityRepository.save(a);
                        }));
    }

    public void addCredentialAcceptedActivity(@NonNull AriesCredential credential) {
        notifyCredentialIssuerActivity(credential, ActivityState.CREDENTIAL_EXCHANGE_ACCEPTED);
    }

    public void addCredentialProblemActivity(@NonNull AriesCredential credential) {
        notifyCredentialIssuerActivity(credential, ActivityState.CREDENTIAL_EXCHANGE_PROBLEM);
    }

    private void notifyCredentialIssuerActivity(@NonNull AriesCredential credential, ActivityState state) {
        partnerRepo.findByConnectionId(credential.getConnectionId())
                .ifPresent(partner -> activityRepository.findByLinkIdAndTypeAndRole(credential.getId(),
                        ActivityType.CREDENTIAL_EXCHANGE,
                        ActivityRole.CREDENTIAL_EXCHANGE_ISSUER).ifPresentOrElse(activity -> {
                            activity.setState(state);
                            activity.setCompleted(true);
                            activityRepository.update(activity);
                            eventPublisher
                                    .publishEventAsync(ActivityNotificationEvent.builder().activity(activity).build());
                        },
                                () -> {
                                    Activity a = Activity.builder()
                                            .linkId(credential.getId())
                                            .partner(partner)
                                            .type(ActivityType.CREDENTIAL_EXCHANGE)
                                            .role(ActivityRole.CREDENTIAL_EXCHANGE_ISSUER)
                                            .state(state)
                                            .completed(true)
                                            .build();
                                    activityRepository.save(a);
                                    eventPublisher
                                            .publishEventAsync(ActivityNotificationEvent.builder().activity(a).build());
                                }));
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
            eventPublisher.publishEventAsync(ActivityNotificationEvent.builder().activity(a).build());
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
                    eventPublisher.publishEventAsync(ActivityNotificationEvent.builder().activity(a).build());
                });
    }

    public void addPresentationExchangeTask(@NonNull PartnerProof partnerProof) {
        // in case event is fired multiple times, see if already exists.
        ActivityRole role = getPresentationExchangeRole(partnerProof);
        ActivityState state = getPresentationExchangeState(partnerProof);

        Optional<Activity> existing = activityRepository.findByLinkIdAndTypeAndRole(partnerProof.getId(),
                ActivityType.PRESENTATION_EXCHANGE,
                role);

        if (existing.isEmpty()) {
            Activity a = Activity.builder()
                    .linkId(partnerProof.getId())
                    .partner(partnerProof.getPartner())
                    .type(ActivityType.PRESENTATION_EXCHANGE)
                    .role(role)
                    .state(state)
                    .completed(ActivityState.PRESENTATION_EXCHANGE_SENT.equals(state))
                    .build();
            activityRepository.save(a);

            if (!a.isCompleted()) {
                // this looks like we created a task!
                eventPublisher.publishEventAsync(TaskAddedEvent.builder().activity(a).build());
            } else {
                eventPublisher.publishEventAsync(ActivityNotificationEvent.builder().activity(a).build());
            }
        }
    }

    public void completePresentationExchangeTask(@NonNull PartnerProof partnerProof) {
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
                            .partner(partnerProof.getPartner())
                            .type(ActivityType.PRESENTATION_EXCHANGE)
                            .role(role)
                            .state(state)
                            .completed(true)
                            .build();
                    activityRepository.save(a);
                });
    }

    public void declinePresentationExchangeTask(@NonNull PartnerProof partnerProof) {
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
        case DECLINED:
            return ActivityState.PRESENTATION_EXCHANGE_DECLINED;
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

    @io.micronaut.core.annotation.NonNull
    private ActivityRole getPresentationExchangeRole(@NonNull PartnerProof partnerProof) {
        if (partnerProof.getRole() == null) {
            throw new IllegalStateException("Partner proof always needs a role set");
        }
        return PresentationExchangeRole.PROVER.equals(partnerProof.getRole())
                ? ActivityRole.PRESENTATION_EXCHANGE_PROVER
                : ActivityRole.PRESENTATION_EXCHANGE_VERIFIER;
    }

}
