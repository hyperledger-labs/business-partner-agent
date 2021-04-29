package org.hyperledger.bpa.model;

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.bpa.api.CredentialType;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bpa_credential_exchange")
public class BPACredentialExchange {

    @Id
    @AutoPopulated
    private UUID id;

    @DateCreated
    private Instant createdAt;

    @OneToOne
    private BPASchema schema;

    @OneToOne
    private BPACredentialDefinition credDef;

    @OneToOne
    private Partner partner;

    @Enumerated(EnumType.STRING)
    private CredentialType type;

    @Nullable
    private String label;

    private String threadId;

    private String credentialExchangeId;

    @Enumerated(EnumType.STRING)
    private CredentialExchangeRole role;

    @Enumerated(EnumType.STRING)
    private CredentialExchangeState state;

    @Nullable
    @TypeDef(type = DataType.JSON)
    private Map<String, Object> credentialOffer;

    @Nullable
    @TypeDef(type = DataType.JSON)
    private Map<String, Object> credentialProposal;

    @Nullable
    @TypeDef(type = DataType.JSON)
    private Map<String, Object> credential;

    private Instant updatedAt;

}
