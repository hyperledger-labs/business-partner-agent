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
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.bpa.config.ActivityLogConfig;
import org.hyperledger.bpa.controller.api.WebSocketMessageBody;
import org.hyperledger.bpa.impl.notification.*;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Slf4j
public class NotificationEventListener {

    @Inject
    PartnerManager partnerManager;

    @Inject
    MessageService messageService;

    @Inject
    ActivityLogConfig activityLogConfig;

    @EventListener
    @Async
    public void onCredentialAddedEvent(CredentialAddedEvent event) {
        log.debug("onCredentialAddedEvent");
        // we have the connection id, but not the partner, will need to look up
        // partner...
        partnerManager.getPartnerByConnectionId(event.getCredential().getConnectionId()).ifPresent(p -> {
            WebSocketMessageBody message = WebSocketMessageBody.notificationEvent(
                    WebSocketMessageBody.WebSocketMessageType.onCredentialAdded,
                    event.getCredential().getId().toString(),
                    event.getCredential(),
                    p);
            messageService.sendMessage(message);

            WebSocketMessageBody task = WebSocketMessageBody.notificationEvent(
                    WebSocketMessageBody.WebSocketMessageType.onNewTask,
                    event.getCredential().getId().toString(),
                    event.getCredential(),
                    p);
            messageService.sendMessage(task);

        });
    }

    @EventListener
    @Async
    public void onPartnerRequestReceivedEvent(PartnerRequestReceivedEvent event) {
        log.debug("onPartnerRequestReceivedEvent");
        // only notify if this is a task (requires manual intervention)
        if (activityLogConfig.getConnectionStatesForTasks().contains(event.getPartner().getState())) {
            WebSocketMessageBody message = WebSocketMessageBody.notificationEvent(
                    WebSocketMessageBody.WebSocketMessageType.onPartnerRequestReceived,
                    event.getPartner().getId(),
                    null,
                    event.getPartner());
            messageService.sendMessage(message);
        }
    }

    @EventListener
    @Async
    public void onPartnerAddedEvent(PartnerAddedEvent event) {
        log.debug("onPartnerAddedEvent");
        WebSocketMessageBody message = WebSocketMessageBody.notificationEvent(
                WebSocketMessageBody.WebSocketMessageType.onPartnerAdded,
                event.getPartner().getId(),
                null,
                event.getPartner());
        messageService.sendMessage(message);
    }

    @EventListener
    @Async
    public void onPartnerRemovedEvent(PartnerRemovedEvent event) {
        log.debug("onPartnerRemovedEvent");
        WebSocketMessageBody message = WebSocketMessageBody.notificationEvent(
                WebSocketMessageBody.WebSocketMessageType.onPartnerRemoved,
                event.getPartner().getId(),
                null,
                event.getPartner());
        messageService.sendMessage(message);
    }

    @EventListener
    @Async
    public void onPresentationRequestCompletedEvent(PresentationRequestCompletedEvent event) {
        log.debug("onPresentationRequestCompletedEvent");
        // we have the partner id, but not the partner, will need to look up partner...
        partnerManager.getPartnerById(event.getProofExchange().getPartnerId()).ifPresent(p -> {
            WebSocketMessageBody message = null;
            if (event.getProofExchange().getRole().equals(PresentationExchangeRole.PROVER)) {
                message = WebSocketMessageBody.notificationEvent(
                        WebSocketMessageBody.WebSocketMessageType.onPresentationProved,
                        event.getProofExchange().getId().toString(),
                        event.getProofExchange(),
                        p);
            } else {
                message = WebSocketMessageBody.notificationEvent(
                        WebSocketMessageBody.WebSocketMessageType.onPresentationVerified,
                        event.getProofExchange().getId().toString(),
                        event.getProofExchange(),
                        p);
            }
            messageService.sendMessage(message);
        });
    }

    @EventListener
    @Async
    public void onPresentationRequestReceivedEvent(PresentationRequestReceivedEvent event) {
        log.debug("onPresentationRequestReceivedEvent");
        // only notify if this is a task (requires manual intervention)
        if (activityLogConfig.getPresentationExchangeStatesForTasks().contains(event.getProofExchange().getState())) {
            // we have the partner id, but not the partner, will need to look up partner...
            partnerManager.getPartnerById(event.getProofExchange().getPartnerId()).ifPresent(p -> {
                WebSocketMessageBody message = WebSocketMessageBody.notificationEvent(
                        WebSocketMessageBody.WebSocketMessageType.onPresentationRequestReceived,
                        event.getProofExchange().getId().toString(),
                        event.getProofExchange(),
                        p);
                messageService.sendMessage(message);

                WebSocketMessageBody task = WebSocketMessageBody.notificationEvent(
                        WebSocketMessageBody.WebSocketMessageType.onNewTask,
                        event.getProofExchange().getId().toString(),
                        event.getProofExchange(),
                        p);
                messageService.sendMessage(task);
            });
        }
    }
}
