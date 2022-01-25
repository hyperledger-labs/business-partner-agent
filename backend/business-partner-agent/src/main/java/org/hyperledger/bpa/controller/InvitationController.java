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
package org.hyperledger.bpa.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.controller.api.invitation.AcceptInvitationRequest;
import org.hyperledger.bpa.controller.api.invitation.CheckInvitationRequest;
import org.hyperledger.bpa.controller.api.partner.CreatePartnerInvitationRequest;
import org.hyperledger.bpa.impl.aries.connection.ConnectionManager;

@Slf4j
@Controller("/api/invitations")
@Tag(name = "Invitation Management")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class InvitationController {

    @Inject
    ConnectionManager cm;

    /**
     * check invitation (receive)
     * 
     * @param body {@link CheckInvitationRequest}
     * @return {@link MutableHttpResponse}
     */
    @Post("/check")
    public MutableHttpResponse<Object> checkInvitation(@Body CheckInvitationRequest body) {
        return HttpResponse.ok(cm.checkReceivedInvitation(body.getInvitationUri()));
    }

    /**
     * receive / accept invitation
     * 
     * @param body {@link AcceptInvitationRequest}
     * @return {@link MutableHttpResponse}
     */
    @Post("/accept")
    public MutableHttpResponse<?> acceptInvitation(@Body AcceptInvitationRequest body) {
        cm.receiveInvitation(body.getInvitationBlock(), body.getAlias(), body.getTag(), body.getTrustPing());
        return HttpResponse.ok();
    }

    /**
     * Create a connection-invitation
     *
     * @param req {@link CreatePartnerInvitationRequest}
     * @return {@link PartnerAPI}
     */
    @Post
    public HttpResponse<?> requestConnectionInvitation(@Body CreatePartnerInvitationRequest req) {
        return HttpResponse.ok(cm.createConnectionInvitation(req));
    }

}
