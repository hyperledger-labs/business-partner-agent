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
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.webhook.EventHandler;
import org.hyperledger.bpa.config.ActivityLogConfig;
import org.hyperledger.bpa.controller.api.WebSocketMessageBody;
import org.hyperledger.bpa.controller.api.activity.ActivityType;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
@NoArgsConstructor
public class ActivitiesEventHandler extends EventHandler {
    @Inject
    MessageService messageService;

    @Inject
    ActivityLogConfig activityLogConfig;

    public void handleConnection(ConnectionRecord connection) {
        Boolean completed = null;
        boolean notify = true;
        if (activityLogConfig.getConnectionStatesForTasks().contains(connection.getState())) {
            completed = false;
            // we do not always want to notify... it is only a task on incoming request
            notify = connection.isIncomingConnection();
        } else if (activityLogConfig.getConnectionStatesCompleted().contains(connection.getState())) {
            completed = true;
        }
        if (completed != null && notify) {
            messageService
                    .sendMessage(WebSocketMessageBody.notification(ActivityType.CONNECTION_REQUEST, completed));
        }
    }

    public void handleProof(PresentationExchangeRecord proof) {
        Boolean completed = null;
        if (activityLogConfig.getPresentationExchangeStatesForTasks().contains(proof.getState())) {
            completed = false;
        } else if (activityLogConfig.getPresentationExchangeStatesCompleted().contains(proof.getState())) {
            completed = true;
        }
        if (completed != null) {
            messageService
                    .sendMessage(WebSocketMessageBody.notification(ActivityType.PRESENTATION_EXCHANGE, completed));
        }
    }
}
