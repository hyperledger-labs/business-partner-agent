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
package org.hyperledger.bpa.controller.api;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.convert.format.Format;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.QueryValue;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.bpa.api.CredentialType;

import java.util.List;

@Data
@NoArgsConstructor
@Introspected
public class PaginationCommand {

    private HttpRequest<?> httpRequest;

    @QueryValue
    private int page;

    @QueryValue
    private Integer size;

    @QueryValue
    private Boolean desc;

    @QueryValue
    @Nullable
    private String q;

    @Nullable
    @Parameter(description = "types filter")
    @QueryValue
    @Format("MULTI")
    private List<CredentialType> types;

    public Pageable toPageable() {
        Sort sort = Sort.of();
        if (StringUtils.isNotEmpty(getQ())) {
            if (getDesc()) {
                sort = Sort.of(Sort.Order.desc(getQ()));
            } else {
                sort = Sort.of(Sort.Order.asc(getQ()));
            }
        }
        return Pageable.from(getPage(), getSize(), sort);
    }

    public Integer getSize() {
        return size != null ? size : -1;
    }

    public Boolean getDesc() {
        return desc != null ? desc : false;
    }
}
