package org.hyperledger.bpa.model;

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bpa_cred_def")
public class BPACredentialDefinition {

    public final static Integer REVOCATION_REGISTRY_MIN_SIZE = 4;
    public final static Integer REVOCATION_REGISTRY_MAX_SIZE = 32768;

    @Id
    @AutoPopulated
    private UUID id;

    @DateCreated
    private Instant createdAt;

    @ManyToOne
    private BPASchema schema;

    private String credentialDefinitionId;

    private String tag;

    private Boolean isSupportRevocation = Boolean.FALSE;

    private Integer revocationRegistrySize = REVOCATION_REGISTRY_MIN_SIZE;

}
