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
package org.hyperledger.bpa.impl.aries.connection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;
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
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.controller.api.invitation.CheckInvitationResponse;
import org.hyperledger.bpa.impl.util.Converter;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class InvitationParser {

    private final OkHttpClient httpClient;

    private static final List<String> paramNames = List.of("c_i", "d_m", "oob");

    static final List<String> CONNECTION_INVITATION_TYPES = List.of(
            "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/connections/1.0/invitation",
            "https://didcomm.org/connections/1.0/invitation");
    static final List<String> OOB_INVITATION_TYPES = List.of(
            "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/out-of-band/1.0/invitation",
            "https://didcomm.org/out-of-band/1.0/invitation");

    public InvitationParser() {
        this.httpClient = new OkHttpClient.Builder().followRedirects(false).build();
    }

    @Inject
    @Setter(AccessLevel.PACKAGE)
    ObjectMapper mapper;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

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

    // take an uri, determine if it is an invitation, and if so, what type and can
    // it be handled?
    public CheckInvitationResponse checkInvitation(@NonNull String invitationUri) {
        HttpUrl url = uriToUrl(invitationUri, true);
        String invitationBlock;
        if (url != null) {
            invitationBlock = parseInvitationBlock(url);
            if (StringUtils.isEmpty(invitationBlock)) {
                invitationBlock = parseInvitationBlockFromRedirect(url);
            }
            if (StringUtils.isEmpty(invitationBlock)) {
                throw new InvitationException(ms.getMessage("api.invitation.url.invalid"));
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
            throw new InvitationException(ms.getMessage("api.invitation.decoding.error"));
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

                    String type = (String) o.get("@type");
                    if (CONNECTION_INVITATION_TYPES.contains(type)) {
                        // Invitation
                        try {
                            Gson gson = GsonConfig.defaultConfig();
                            ReceiveInvitationRequest r = gson.fromJson(decodedBlock, ReceiveInvitationRequest.class);
                            invitation.setInvitationRequest(r);
                        } catch (Exception e) {
                            String msg = ms.getMessage("api.invitation.decoding.error.not.v1");
                            invitation.setError(msg);
                            log.error(msg);
                        }

                    } else if (OOB_INVITATION_TYPES.contains(type)) {
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
                                String msg = ms.getMessage("api.invitation.decoding.error.not.oob");
                                invitation.setError(msg);
                                log.error(msg);
                            }
                        }
                    } else {
                        String msg = ms.getMessage("api.invitation.decoding.error.unsupported.type",
                                Map.of("type", o.get("@type")));
                        invitation.setError(msg);
                        log.error(msg);
                    }
                } catch (JsonProcessingException e) {
                    String msg = ms.getMessage("api.invitation.parsing.error", Map.of("message", e.getMessage()));
                    invitation.setError(msg);
                    log.error(msg, e);
                }
            } else {
                String msg = ms.getMessage("api.invitation.decoding.error.empty.result");
                invitation.setError(msg);
                log.error(msg);
            }
        } else {
            String msg = ms.getMessage("api.invitation.empty");
            invitation.setError(msg);
            log.error(msg);
        }
        return invitation;
    }

    private String parseInvitationBlock(@NonNull HttpUrl url) {
        for (String name : paramNames) {
            String invitationBlock = url.queryParameter(name);
            if (StringUtils.isNotEmpty(invitationBlock)) {
                // TODO not good
                return invitationBlock.replace(" ", "+");
            }
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
                        HttpUrl locationUrl = uriToUrl(location, false);
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

    /**
     * The invitation can be in the form of, uri, url, or redirect. In case of uri
     * we are only interested in the query so the result is wrapped for easier
     * parsing. In case of an url we simply convert to maintain the original
     * location.
     * 
     * @param invitationUri uri as String
     * @param decode        if the invitationUri needs url decoding
     * @return {@link HttpUrl} or null
     */
    private HttpUrl uriToUrl(String invitationUri, boolean decode) {
        HttpUrl result = null;
        try {
            String decodedUri = invitationUri;
            if (decode) {
                decodedUri = URLDecoder.decode(invitationUri, StandardCharsets.UTF_8);
            }
            URI uri = new URI(decodedUri);
            if (uri.getScheme().startsWith("http")) {
                result = HttpUrl.parse(decodedUri);
            } else {
                String query = uri.getQuery();
                if (StringUtils.isNotEmpty(query)) {
                    result = HttpUrl.parse("https://placeholder.co?" + query);
                }
            }
        } catch (URISyntaxException e) {
            throw new InvitationException(ms.getMessage("api.invitation.url.invalid"));
        }
        return result;
    }

}
