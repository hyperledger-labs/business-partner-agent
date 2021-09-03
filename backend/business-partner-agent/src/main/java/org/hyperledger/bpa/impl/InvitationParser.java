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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest;
import org.hyperledger.aries.api.out_of_band.InvitationMessage;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.api.exception.InvitationException;
import org.hyperledger.bpa.controller.api.invitation.CheckInvitationResponse;
import org.hyperledger.bpa.impl.util.Converter;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@NoArgsConstructor
@Singleton
public class InvitationParser {

    OkHttpClient httpClient = new OkHttpClient.Builder().followRedirects(false).build();

    @Inject
    @Setter(AccessLevel.PACKAGE)
    ObjectMapper mapper;

    @Data
    public static final class Invitation {
        private boolean oob;
        private boolean parsed;
        private ReceiveInvitationRequest invitationRequest;
        private InvitationMessage<?> invitationMessage;
        private String error;
        private String invitationBlock;
        private Map<String, Object> invitation;
    }

    // take an url, determine if it is an invitation, and if so, what type and can
    // it be handled?
    public CheckInvitationResponse checkInvitation(@NonNull String invitationUrl) {
        HttpUrl url = HttpUrl.parse(URLDecoder.decode(invitationUrl, StandardCharsets.UTF_8));
        String invitationBlock;
        if (url != null) {
            invitationBlock = parseInvitationBlock(url);
            if (StringUtils.isEmpty(invitationBlock)) {
                invitationBlock = parseInvitationBlockFromRedirect(url);
            }
            if (StringUtils.isEmpty(invitationBlock)) {
                throw new InvitationException("Invitation Url does not contain a known or valid invitation.");
            }

            Invitation invite = parseInvitation(invitationBlock);

            if (StringUtils.isNotEmpty(invite.getError())) {
                throw new InvitationException(invite.getError());
            } else {
                if (invite.isParsed()) {
                    if (invite.getInvitationRequest() != null) {
                        ReceiveInvitationRequest r = invite.getInvitationRequest();
                        return CheckInvitationResponse.builder()
                                .label(r.getLabel())
                                .invitation(invite.getInvitation())
                                .invitationBlock(invite.getInvitationBlock())
                                .build();
                    } else if (invite.getInvitationMessage() != null) {
                        return CheckInvitationResponse.builder()
                                .label(invite.getInvitationMessage().getLabel())
                                .invitation(invite.getInvitation())
                                .invitationBlock(invite.getInvitationBlock())
                                .build();
                    }
                }
            }
        } else {
            throw new InvitationException("Invitation Url could not be decoded. Cannot determine invitation details.");
        }
        return null;
    }

    public Invitation parseInvitation(String invitationBlock) {
        Invitation invitation = new Invitation();
        if (StringUtils.isNotEmpty(invitationBlock)) {

            invitation.setInvitationBlock(invitationBlock);

            String decodedBlock = decodeInvitationBlock(invitationBlock);
            if (StringUtils.isNotEmpty(decodedBlock)) {
                Map<String, Object> o;
                try {
                    o = mapper.readValue(decodedBlock, Converter.STRING_OBJECT_MAP);
                    invitation.setInvitation(o);
                    invitation.setParsed(true);

                    if ("did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/connections/1.0/invitation".equals(o.get("@type"))) {
                        // Invitation
                        try {
                            Gson gson = GsonConfig.defaultConfig();
                            ReceiveInvitationRequest r = gson.fromJson(decodedBlock, ReceiveInvitationRequest.class);
                            invitation.setInvitationRequest(r);
                        } catch (Exception e) {
                            String msg = "Expecting a valid Connections 1.0 invitation; could not parse data.";
                            invitation.setError(msg);
                            log.error(msg);
                        }

                    } else if ("did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/out-of-band/1.0/invitation"
                            .equals(o.get("@type"))) {
                        invitation.setOob(true);

                        Gson gson = GsonConfig.defaultConfig();
                        try {
                            InvitationMessage<InvitationMessage.InvitationMessageService> im = gson
                                    .fromJson(decodedBlock, InvitationMessage.RFC0067_TYPE);
                            invitation.setInvitationMessage(im);
                        } catch (JsonSyntaxException e) {
                            try {
                                InvitationMessage<String> im = gson.fromJson(decodedBlock,
                                        InvitationMessage.STRING_TYPE);
                                invitation.setInvitationMessage(im);
                            } catch (JsonSyntaxException ex) {
                                String msg = "Expecting a valid Out Of Band 1.0 invitation; could not parse data.";
                                invitation.setError(msg);
                                log.error(msg);
                            }
                        }
                    } else {
                        String msg = String.format("Unknown or unsupported Invitation type. @type = '%s'",
                                o.get("@type"));
                        invitation.setError(msg);
                        log.error(msg);
                    }
                } catch (JsonProcessingException e) {
                    String msg = String.format("Error parsing invitation %s", e.getMessage());
                    invitation.setError(msg);
                    log.error(msg, e);
                }
            } else {
                String msg = "Invitation could not be decoded; result was empty";
                invitation.setError(msg);
                log.error(msg);
            }
        } else {
            String msg = "Invitation was empty";
            invitation.setError(msg);
            log.error(msg);
        }
        return invitation;
    }

    private String parseInvitationBlock(@NonNull HttpUrl url) {
        List<String> paramNames = List.of("c_i", "d_m", "oob");
        for (String name : paramNames) {
            String invitationBlock = url.queryParameter(name);
            if (StringUtils.isNotEmpty(invitationBlock))
                return invitationBlock;
        }
        return null;
    }

    private String decodeInvitationBlock(String invitationBlock) {
        if (StringUtils.isNotEmpty(invitationBlock)) {
            byte[] decodedBlockBytes = Base64.getDecoder().decode(invitationBlock);
            return new String(decodedBlockBytes, StandardCharsets.UTF_8);
        }
        return null;
    }

    private String parseInvitationBlockFromRedirect(@NonNull HttpUrl url) {
        // in this case, we are going to get the url and see if we can get a redirect to
        // another url that contains invitation
        try {
            Request request = new Request.Builder().url(url).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isRedirect()) {
                    String location = response.header("location");
                    if (StringUtils.isNotEmpty(location)) {
                        HttpUrl locationUrl = HttpUrl.parse(location);
                        if (locationUrl != null)
                            return parseInvitationBlock(locationUrl);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

}
