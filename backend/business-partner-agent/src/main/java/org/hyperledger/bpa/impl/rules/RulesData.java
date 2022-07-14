package org.hyperledger.bpa.impl.rules;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.connection.ConnectionTheirRole;
import org.hyperledger.bpa.api.notification.Event;
import org.hyperledger.bpa.controller.api.partner.UpdatePartnerRequest;
import org.hyperledger.bpa.persistence.model.ActiveRules;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.Tag;

import java.util.List;
import java.util.Optional;
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
            @JsonSubTypes.Type(value = Trigger.ProofReceivedTrigger.class, name = Trigger.PROOF_RECEIVED_TRIGGER_NAME),
            @JsonSubTypes.Type(value = Trigger.EventTrigger.class, name = Trigger.EVENT_TRIGGER_NAME)
    })
    @NoArgsConstructor
    public abstract static class Trigger {

        public static final String CONNECTION_TRIGGER_NAME = "connection";
        public static final String PROOF_RECEIVED_TRIGGER_NAME = "proof_received";
        public static final String EVENT_TRIGGER_NAME = "event";

        abstract boolean apply(EventContext ctx);

        boolean apply(Event event) {
            return false;
        }

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

        @JsonTypeName(Trigger.EVENT_TRIGGER_NAME)
        @Builder
        @Data
        @EqualsAndHashCode(callSuper = true)
        @NoArgsConstructor
        @AllArgsConstructor
        public static class EventTrigger extends Trigger {
            private String eventClassName;

            @Override
            public boolean apply(EventContext ctx) {
                return false;
            }

            @Override
            public boolean apply(Event event) {
                return event.getClass().getSimpleName().equals(this.eventClassName);
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
                Optional<Partner> partner = ctx.getPartnerRepo().findById(ctx.getPartner().getId());

                if (partner.isPresent()) {
                    this.connectionId = partner.get().getConnectionId();
                    Optional<Tag> t1 = ctx.getTagRepo().findByName(tag);
                    Tag t2;
                    if (t1.isPresent()) {
                        t2 = t1.get();
                    } else {
                        t2 = ctx.getTagRepo().save(Tag
                                .builder()
                                .name(tag)
                                .build());
                    }
                    ctx.getPartnerManager().updatePartner(partner.get().getId(),
                            UpdatePartnerRequest.builder().tag(List.of(t2)).build());
                }
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