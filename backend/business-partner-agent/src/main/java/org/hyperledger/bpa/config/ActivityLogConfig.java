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
package org.hyperledger.bpa.config;

import lombok.NoArgsConstructor;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
@NoArgsConstructor
public class ActivityLogConfig {
    /*
     * For now, we are not sure what to do with this configuration. Will each
     * installation determine what their own definition of "complete" is, or which
     * events will be "tasks"? And tasks are going to be dependant in the
     * AcaPyConfig, which flags are set to auto respond...
     */

    @Inject
    AcaPyConfig acaPyConfig;

    public List<ConnectionState> getConnectionStatesForActivities() {
        return connectionStates(ConnectionState.REQUEST, ConnectionState.INVITATION, ConnectionState.ACTIVE,
                ConnectionState.RESPONSE);
    }

    public List<ConnectionState> getConnectionStatesForTasks() {
        if (this.isConnectionRequestTask()) {
            return connectionStates(ConnectionState.REQUEST);
        }
        return List.of();
    }

    public List<ConnectionState> getConnectionStatesCompleted() {
        return connectionStates(ConnectionState.ACTIVE, ConnectionState.RESPONSE);
    }

    public boolean isConnectionRequestTask() {
        return !acaPyConfig.getAutoAcceptRequests();
    }

    public List<PresentationExchangeState> getPresentationExchangeStatesForActivities() {
        return presentationExchangeStates(PresentationExchangeState.REQUEST_RECEIVED,
                PresentationExchangeState.REQUEST_SENT,
                PresentationExchangeState.VERIFIED,
                PresentationExchangeState.PRESENTATION_ACKED);
    }

    public List<PresentationExchangeState> getPresentationExchangeStatesForTasks() {
        if (this.isPresentationExchangeTask()) {
            return presentationExchangeStates(PresentationExchangeState.REQUEST_RECEIVED);
        }
        return List.of();
    }

    public List<PresentationExchangeState> getPresentationExchangeStatesCompleted() {
        return presentationExchangeStates(PresentationExchangeState.VERIFIED,
                PresentationExchangeState.PRESENTATION_ACKED);
    }

    public boolean isPresentationExchangeTask() {
        return !acaPyConfig.getAutoRespondPresentationRequest();
    }

    private List<ConnectionState> connectionStates(ConnectionState... states) {
        return List.of(states);
    }

    private List<PresentationExchangeState> presentationExchangeStates(PresentationExchangeState... states) {
        return List.of(states);
    }

}
