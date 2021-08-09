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
package org.hyperledger.bpa.model;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.DataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.controller.api.partner.PartnerCredentialType;

import io.micronaut.core.annotation.Nullable;

import javax.persistence.*;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.time.Instant;
import java.util.*;

/**
 * Flat representation of a partner. In the web context a partner is just a
 * reference to a did and the public profile that's referenced by the did
 * documents profile endpoint. In the context of aries a partner also becomes a
 * connection including pairwise did's, states, trust ping etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Entity
public class Partner {

    @Id
    @AutoPopulated
    private UUID id;

    @DateCreated
    private Instant createdAt;

    @DateUpdated
    private Instant updatedAt;

    /** The last time a ping response was received from the partner */
    @Nullable
    private Instant lastSeen;

    /** The fully qualified did like did:sov:123 */
    private String did;

    /**
     * If the partner supports aries, set if the partner has a endpoint that
     * supports did communication
     */
    private Boolean ariesSupport;

    /** aries connection id */
    @Nullable
    private String connectionId;

    /** Aries connection state */
    @Nullable
    @Enumerated(EnumType.STRING)
    private ConnectionState state;

    /**
     * Aries connection label, if incoming connection set by the partner via the
     * --label flag, or through rest overwrite
     */
    @Nullable
    private String label;

    /** The partners alias or name, always set by a user in the UI */
    @Nullable
    private String alias;

    /** Direction of the communication, incoming or outgoing aries connection */
    @Nullable
    private Boolean incoming;

    /** If the partners public profile is valid */
    @Nullable
    private Boolean valid;

    /** If the trust ping feature is active for this partner */
    @Nullable
    private Boolean trustPing;

    /** Aries OOB invitation message id */
    @Nullable
    private String invitationMsgId;

    /**
     * The Partners Public Profile {@link VerifiablePresentation} to be used in the
     * {@link PartnerAPI}
     */
    @Nullable
    @TypeDef(type = DataType.JSON)
    private Map<String, Object> verifiablePresentation;

    /**
     * Serialized {@link PartnerCredentialType} to allow filtering partners by
     * supported credentials
     */
    @Nullable
    @TypeDef(type = DataType.JSON)
    private Map<String, Object> supportedCredentials;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.REFRESH })
    @JoinTable(name = "partner_tag")
    private Set<Tag> tags = new HashSet<>();

    @Transient
    public boolean hasConnectionId() {
        return connectionId != null;
    }

}
