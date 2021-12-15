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
package org.hyperledger.bpa.api.aries;

import io.micronaut.core.util.CollectionUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.acy_py.generated.model.TransactionRecord;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.impl.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionAPI {

    private UUID transactionId;

    private UUID connectionId;

    private Boolean endorserWriteTxn;

    private List<Object> formats;

    private List<Object> messagesAttach;

    private List<Object> signatureRequest;

    private List<Object> signatureResponse;

    private String state;

    private UUID threadId;

    private Object timing;

    private Long createdAt;

    private Long updatedAt;

    public static TransactionAPI from(TransactionRecord s) {
        TransactionAPIBuilder builder = TransactionAPI.builder()
                .transactionId(UUID.fromString(s.getTransactionId()))
                .connectionId(UUID.fromString(s.getConnectionId()))
                .endorserWriteTxn(s.getEndorserWriteTxn())
                // TODO convert from List<Map<String, String>> to appropriate object
                // .formats(s.getFormats())
                // .messagesAttach(s.getMessagesAttach())
                // .signatureRequest(s.getSignatureRequest())
                // .signatureResponse(s.getSignatureResponse())
                .state(s.getState())
                // .threadId(UUID.fromString(s.getThreadId()))
                .timing(s.getTiming())
                .createdAt(TimeUtil.fromISOInstant(s.getCreatedAt()).toEpochMilli())
                .updatedAt(TimeUtil.fromISOInstant(s.getUpdatedAt()).toEpochMilli());
        return builder.build();
    }

}
