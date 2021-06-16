package org.hyperledger.bpa.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.model.Tag;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagAPI {

    private UUID id;

    private String name;

    private Boolean isReadOnly;

    public static TagAPI from(Tag t) {
        TagAPI.TagAPIBuilder builder = TagAPI.builder();
        return builder
                .id(t.getId())
                .name(t.getName())
                .isReadOnly(t.getIsReadOnly())
                .build();
    }
}
