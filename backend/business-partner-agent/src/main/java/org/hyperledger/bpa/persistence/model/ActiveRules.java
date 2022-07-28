package org.hyperledger.bpa.persistence.model;

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import lombok.*;
import lombok.experimental.Accessors;
import org.hyperledger.bpa.impl.rules.RulesData;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "active_rules")
@Accessors(chain = true)
public class ActiveRules {

    @Id
    @AutoPopulated
    private UUID id;

    @DateCreated
    private Instant createdAt;

    @DateUpdated
    private Instant updatedAt;

    @TypeDef(type = DataType.JSON)
    private RulesData.Trigger trigger;

    @TypeDef(type = DataType.JSON)
    private RulesData.Action action;
}
