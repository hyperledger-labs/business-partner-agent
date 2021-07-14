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

import io.micronaut.context.event.ApplicationEventListener;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.bpa.impl.StartupTasks;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Getter
@Singleton
@NoArgsConstructor
@Slf4j
public class AcaPyConfig implements ApplicationEventListener<StartupTasks.AcaPyReady> {

    @Inject
    AriesClient ac;

    Boolean autoRespondCredentialOffer;
    Boolean autoRespondCredentialProposal;
    Boolean autoRespondCredentialRequest;
    Boolean autoRespondPresentationProposal;
    Boolean autoRespondPresentationRequest;
    Boolean autoVerifyPresentation;
    Boolean autoStoreCredential;
    Boolean autoAcceptInvites;
    Boolean autoAcceptRequests;
    Boolean autoRespondMessages;

    @Override
    public void onApplicationEvent(StartupTasks.AcaPyReady event) {
        try {
            ac.statusConfig().ifPresent(adminConfig -> {
                autoAcceptInvites = adminConfig.getAs("debug.auto_accept_invites", Boolean.class).isPresent();
                autoAcceptRequests = adminConfig.getAs("debug.auto_accept_requests", Boolean.class).isPresent();
                autoRespondMessages = adminConfig.getAs("debug.auto_respond_messages", Boolean.class).isPresent();
                autoRespondCredentialOffer = adminConfig.getAs("debug.auto_respond_credential_offer", Boolean.class)
                        .isPresent();
                autoRespondCredentialProposal = adminConfig
                        .getAs("debug.auto_respond_credential_proposal", Boolean.class).isPresent();
                autoRespondCredentialRequest = adminConfig.getAs("debug.auto_respond_credential_request", Boolean.class)
                        .isPresent();
                autoRespondPresentationProposal = adminConfig
                        .getAs("debug.auto_respond_presentation_proposal", Boolean.class).isPresent();
                autoRespondPresentationRequest = adminConfig
                        .getAs("debug.auto_respond_presentation_request", Boolean.class).isPresent();
                autoStoreCredential = adminConfig.getAs("debug.auto_store_credential", Boolean.class).isPresent();
                autoVerifyPresentation = adminConfig.getAs("debug.auto_verify_presentation", Boolean.class).isPresent();
            });
        } catch (IOException e) {
            log.warn("No aca-py");
        }
    }
}
