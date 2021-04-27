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

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import io.micronaut.data.annotation.TypeDef;
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
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Flat representation of a partner. In the web context a partner is just a
 * reference to a did and the public profile thats referenced via the did
 * documents profile endpoint. In the context of aries a partner also becomes a
 * connection including pairwise did's and states etc.
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

    @Nullable
    private Instant lastSeen; // last time a ping response was received from the partner

    /** The fully qualified did like did:sov:123 */
    private String did;

    private Boolean ariesSupport;

    @Nullable
    private String connectionId; // aries connection id

    @Nullable
    private ConnectionState state; // aries connection state

    @Nullable
    private String label; // aries connection label
                          // if incoming connection set by the partner, if outgoing set by us to match
                          // connection events

    @Nullable
    private String alias; // aries connection alias

    @Nullable
    private Boolean incoming; // incoming aries connection

    @Nullable
    private Boolean valid; // if the public profile is valid

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

    @Transient
    public boolean hasConnectionId() {
        return connectionId != null;
    }

}
