package org.hyperledger.bpa.model;

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.jdbc.annotation.ColumnTransformer;
import io.micronaut.data.model.DataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.bpa.api.CredentialType;

import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
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

    private CredentialType type;

    @Nullable
    private String label;

    private String threadId;

    private String credentialExchangeId;

    @ColumnTransformer(write = "LOWER(?)", read = "LOWER(@.role)")
    private String role;

    @ColumnTransformer(write = "LOWER(?)", read = "LOWER(@.state)")
    private String state;

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
