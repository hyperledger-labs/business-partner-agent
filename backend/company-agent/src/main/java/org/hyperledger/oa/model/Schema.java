package org.hyperledger.oa.model;

import java.time.Instant;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hyperledger.oa.api.CredentialType;

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Schema {

    @Id
    @AutoPopulated
    private UUID id;

    @DateCreated
    private Instant createdAt;

    @Nullable
    private String label;

    private CredentialType type;

    private String schemaId;

    private Integer seqNo;
}
