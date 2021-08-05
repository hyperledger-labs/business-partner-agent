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

import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.config.ActivityLogConfig;
import org.hyperledger.bpa.controller.api.WebSocketMessageBody;
import org.hyperledger.bpa.impl.notification.*;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.PartnerProof;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
@Slf4j
public class NotificationEventListener {

    @Inject
    PartnerManager partnerManager;

    @Inject
    MessageService messageService;

    @Inject
    ActivityLogConfig activityLogConfig;

    @Inject
    Converter conv;

    @Inject
    ActivityManager activityManager;

    @EventListener
    @Async
    public void onCredentialAddedEvent(CredentialAddedEvent event) {
        log.debug("onCredentialAddedEvent");
        // we have the connection id, but not the partner, will need to look up
        // partner...
        PartnerAPI partnerAPI = partnerManager.getPartnerByConnectionId(event.getCredential().getConnectionId());
        if (partnerAPI != null) {
            WebSocketMessageBody message = WebSocketMessageBody.notificationEvent(
                    WebSocketMessageBody.WebSocketMessageType.ON_CREDENTIAL_ADDED,
                    event.getCredential().getId().toString(),
                    event.getCredential(),
                    partnerAPI);
            messageService.sendMessage(message);
        }
    }

    @EventListener
    @Async
    public void onPartnerRequestCompletedEvent(PartnerRequestCompletedEvent event) {
        log.debug("onPartnerRequestCompletedEvent");
        WebSocketMessageBody message = WebSocketMessageBody.notificationEvent(
                WebSocketMessageBody.WebSocketMessageType.ON_PARTNER_REQUEST_COMPLETED,
                event.getPartner().getId().toString(),
                null,
                conv.toAPIObject(event.getPartner()));
        messageService.sendMessage(message);

        activityManager.completePartnerRequestTask(event.getPartner());
    }

    @EventListener
    @Async
    public void onPartnerRequestReceivedEvent(PartnerRequestReceivedEvent event) {
        log.debug("onPartnerRequestReceivedEvent");
        // only notify if this is a task (requires manual intervention)
        if (activityLogConfig.getConnectionStatesForTasks().contains(event.getPartner().getState())) {

            activityManager.addPartnerRequestReceivedTask(event.getPartner());

            WebSocketMessageBody message = WebSocketMessageBody.notificationEvent(
                    WebSocketMessageBody.WebSocketMessageType.ON_PARTNER_REQUEST_RECEIVED,
                    event.getPartner().getId().toString(),
                    null,
                    conv.toAPIObject(event.getPartner()));
            messageService.sendMessage(message);
        }
    }

    @EventListener
    @Async
    public void onPartnerAddedEvent(PartnerAddedEvent event) {
        log.debug("onPartnerAddedEvent");
        WebSocketMessageBody message = WebSocketMessageBody.notificationEvent(
                WebSocketMessageBody.WebSocketMessageType.ON_PARTNER_ADDED,
                event.getPartner().getId().toString(),
                null,
                conv.toAPIObject(event.getPartner()));
        messageService.sendMessage(message);

        activityManager.addPartnerAddedActivity(event.getPartner());
    }

    @EventListener
    @Async
    public void onPartnerAcceptedEvent(PartnerAcceptedEvent event) {
        log.debug("onPartnerAcceptedEvent");
        WebSocketMessageBody message = WebSocketMessageBody.notificationEvent(
                WebSocketMessageBody.WebSocketMessageType.ON_PARTNER_ACCEPTED,
                event.getPartner().getId().toString(),
                null,
                conv.toAPIObject(event.getPartner()));
        messageService.sendMessage(message);

        activityManager.addPartnerAcceptedActivity(event.getPartner());
    }

    @EventListener
    @Async
    public void onPartnerRemovedEvent(PartnerRemovedEvent event) {
        log.debug("onPartnerRemovedEvent");
        WebSocketMessageBody message = WebSocketMessageBody.notificationEvent(
                WebSocketMessageBody.WebSocketMessageType.ON_PARTNER_REMOVED,
                event.getPartner().getId().toString(),
                null,
                conv.toAPIObject(event.getPartner()));
        messageService.sendMessage(message);

        activityManager.deletePartnerActivities(event.getPartner());
    }

    @EventListener
    @Async
    public void onPresentationRequestCompletedEvent(PresentationRequestCompletedEvent event) {
        log.debug("onPresentationRequestCompletedEvent");
        // we have the partner id, but not the partner, will need to look up partner...
        partnerManager.getPartnerById(event.getPartnerProof().getPartnerId()).ifPresent(p -> {
            WebSocketMessageBody message = null;
            if (event.getPartnerProof().getRole().equals(PresentationExchangeRole.PROVER)) {
                message = WebSocketMessageBody.notificationEvent(
                        WebSocketMessageBody.WebSocketMessageType.ON_PRESENTATION_PROVED,
                        event.getPartnerProof().getId().toString(),
                        conv.toAPIObject(event.getPartnerProof()),
                        p);
            } else {
                message = WebSocketMessageBody.notificationEvent(
                        WebSocketMessageBody.WebSocketMessageType.ON_PRESENTATION_VERIFIED,
                        event.getPartnerProof().getId().toString(),
                        conv.toAPIObject(event.getPartnerProof()),
                        p);
            }
            activityManager.completePresentationExchangeTask(event.getPartnerProof());
            messageService.sendMessage(message);
        });
    }

    @EventListener
    @Async
    public void onPresentationRequestDeclinedEvent(PresentationRequestDeclinedEvent event) {
        log.debug("onPresentationRequestDeclinedEvent");
        handlePresentationRequestEvent(event.getPartnerProof(),
                WebSocketMessageBody.WebSocketMessageType.ON_PRESENTATION_REQUEST_DECLINED);
        activityManager.declinePresentationExchangeTask(event.getPartnerProof());
    }

    @EventListener
    @Async
    public void onPresentationRequestDeletedEvent(PresentationRequestDeletedEvent event) {
        log.debug("onPresentationRequestDeletedEvent");
        handlePresentationRequestEvent(event.getPartnerProof(),
                WebSocketMessageBody.WebSocketMessageType.ON_PRESENTATION_REQUEST_DELETED);
        activityManager.deletePresentationExchangeTask(event.getPartnerProof());
    }

    @EventListener
    @Async
    public void onPresentationRequestReceivedEvent(PresentationRequestReceivedEvent event) {
        log.debug("onPresentationRequestReceivedEvent");
        handlePresentationRequestEvent(event.getPartnerProof(),
                WebSocketMessageBody.WebSocketMessageType.ON_PRESENTATION_REQUEST_RECEIVED);
    }

    @EventListener
    @Async
    public void onPresentationRequestSentEvent(PresentationRequestSentEvent event) {
        log.debug("onPresentationRequestSentEvent");
        handlePresentationRequestEvent(event.getPartnerProof(),
                WebSocketMessageBody.WebSocketMessageType.ON_PRESENTATION_REQUEST_SENT);
    }

    @EventListener
    @Async
    public void onBasicMessageReceivedEvent(BasicMessageReceivedEvent event) {
        log.debug("onBasicMessageReceivedEvent");
        // refactor once we have persistence of messages...
        PartnerAPI partner = partnerManager.getPartnerByConnectionId(event.getMessage().getConnectionId());
        if (partner != null) {
            WebSocketMessageBody message = WebSocketMessageBody.message(partner, event.getMessage());
            messageService.sendMessage(message);
        }
    }

    @EventListener
    @Async
    public void onTaskAddedEvent(TaskAddedEvent event) {
        log.debug("onTaskAddedEvent");
        WebSocketMessageBody task = WebSocketMessageBody.notificationEvent(
                WebSocketMessageBody.WebSocketMessageType.TASK_ADDED,
                event.getActivity().getId().toString(),
                event.getActivity(),
                conv.toAPIObject(event.getActivity().getPartner()));
        messageService.sendMessage(task);
    }

    @EventListener
    @Async
    public void onTaskCompletedEvent(TaskCompletedEvent event) {
        log.debug("onTaskCompletedEvent");
        WebSocketMessageBody task = WebSocketMessageBody.notificationEvent(
                WebSocketMessageBody.WebSocketMessageType.TASK_COMPLETED,
                event.getActivity().getId().toString(),
                event.getActivity(),
                conv.toAPIObject(event.getActivity().getPartner()));
        messageService.sendMessage(task);
    }

    private void handlePresentationRequestEvent(@NonNull PartnerProof partnerProof,
            WebSocketMessageBody.WebSocketMessageType messageType) {
        Optional<PartnerAPI> partnerAPI = partnerManager.getPartnerById(partnerProof.getPartnerId());
        if (partnerAPI.isPresent()) {
            PartnerAPI p = partnerAPI.get();

            activityManager.addPresentationExchangeTask(partnerProof);

            // only notify if this is a task (requires manual intervention)
            if (activityLogConfig.getPresentationExchangeStatesForTasks().contains(partnerProof.getState())) {
                // we have the partner id, but not the partner, will need to look up partner...
                WebSocketMessageBody message = WebSocketMessageBody.notificationEvent(
                        messageType,
                        partnerProof.getId().toString(),
                        conv.toAPIObject(partnerProof),
                        p);
                messageService.sendMessage(message);
            }
        }
    }

}
