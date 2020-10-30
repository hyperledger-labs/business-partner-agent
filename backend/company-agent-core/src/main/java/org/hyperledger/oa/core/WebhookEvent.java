package org.hyperledger.oa.core;

import org.hyperledger.oa.core.RegisteredWebhook.WebhookEventType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent<T> {

    private WebhookEventType type;
    private Long sent;
    private T payload;

}
