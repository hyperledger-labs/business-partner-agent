/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/business-partner-agent

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.hyperledger.oa.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.oa.controller.api.wallet.WalletDocumentRequest;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyDocumentAPI {
    private UUID id;
    private Long createdDate;
    private Long updatedDate;
    private CredentialType type;
    private String schemaId;
    private Boolean isPublic;
    private String label;
    @Schema(example = "{}")
    private JsonNode documentData; // TODO rename to document

    public static MyDocumentAPI fromRequest(WalletDocumentRequest req) {
        return MyDocumentAPI.builder()
                // never set id or createdDate here!
                .type(req.getType())
                .schemaId(req.getSchemaId())
                .isPublic(req.getIsPublic())
                .label(req.getLabel())
                .documentData(req.getDocument())
                .build();
    }
}
