package org.hyperledger.bpa.model;

import io.micronaut.data.annotation.AutoPopulated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Tag {

    @Id
    @AutoPopulated
    private UUID id;

    private String name;

    private Boolean isReadOnly;

}