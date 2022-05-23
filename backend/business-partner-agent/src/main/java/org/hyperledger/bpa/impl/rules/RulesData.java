package org.hyperledger.bpa.impl.rules;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jdk.jfr.Event;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.connection.ConnectionTheirRole;
import org.hyperledger.bpa.persistence.model.ActiveRules;
import java.util.UUID;

@Slf4j
@Data
@Builder
public class RulesData {

    private UUID ruleId;
    private Trigger trigger;
    private Action action;

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Trigger.ConnectionTrigger.class, name = Trigger.CONNECTION_TRIGGER_NAME),
            @JsonSubTypes.Type(value = Trigger.ProofReceivedTrigger.class, name = Trigger.PROOF_RECEIVED_TRIGGER_NAME)
    })
    @NoArgsConstructor
    public abstract static class Trigger {

        public static final String CONNECTION_TRIGGER_NAME = "connection";
        public static final String PROOF_RECEIVED_TRIGGER_NAME = "proof_received";

        abstract boolean apply(EventContext ctx);

        @SuppressWarnings("unused")
        private String type;

        @JsonTypeName(Trigger.CONNECTION_TRIGGER_NAME)
        @Builder
        @Data
        @EqualsAndHashCode(callSuper = true)
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ConnectionTrigger extends Trigger {
            private ConnectionTheirRole role;
            private String tag;
            private String goalCode;

            @Override
            public boolean apply(EventContext ctx) {
                ConnectionRecord connRec = ctx.getConnRec();
                boolean apply = connRec != null && ConnectionState.REQUEST.equals(connRec.getState());
                if (role != null) {
                    apply = apply && role.equals(connRec.getTheirRole());
                }
                return apply;
            }
        }

        @JsonTypeName(Trigger.PROOF_RECEIVED_TRIGGER_NAME)
        @Builder
        @Data
        @EqualsAndHashCode(callSuper = true)
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ProofReceivedTrigger extends Trigger {
            private String tag;
            private UUID proofTemplateId;

            @Override
            public boolean apply(EventContext ctx) {
                return false;
            }
        }
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Action.TagConnection.class, name = Action.TAG_CONNECTION_ACTION_NAME),
            @JsonSubTypes.Type(value = Action.SendProofRequest.class, name = Action.SEND_PROOF_REQUEST_ACTION_NAME)
    })
    @NoArgsConstructor
    public abstract static class Action {

        public static final String TAG_CONNECTION_ACTION_NAME = "tag_connection";
        public static final String SEND_PROOF_REQUEST_ACTION_NAME = "send_proof_request";

        abstract void run(EventContext ctx);

        @SuppressWarnings("unused")
        private String type;

        @JsonTypeName(Action.TAG_CONNECTION_ACTION_NAME)
        @Builder
        @Data
        @EqualsAndHashCode(callSuper = true)
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TagConnection extends Action {
            private String connectionId;
            private String tag;

            @Override
            void run(EventContext ctx) {
                log.debug("tag: {}", tag);
            }
        }

        @JsonTypeName(Action.SEND_PROOF_REQUEST_ACTION_NAME)
        @Builder
        @Data
        @EqualsAndHashCode(callSuper = true)
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SendProofRequest extends Action {
            private String connectionId;
            private UUID proofTemplateId;

            @Override
            void run(EventContext ctx) {

            }
        }
    }

    public static RulesData fromActive(@NonNull ActiveRules ar) {
        return RulesData.builder()
                .ruleId(ar.getId())
                .trigger(ar.getTrigger())
                .action(ar.getAction())
                .build();
    }
}