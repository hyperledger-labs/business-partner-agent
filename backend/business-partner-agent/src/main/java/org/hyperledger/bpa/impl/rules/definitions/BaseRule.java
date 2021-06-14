package org.hyperledger.bpa.impl.rules.definitions;

import com.deliveredtechnologies.rulebook.model.rulechain.cor.CoRRuleBook;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data @NoArgsConstructor
public abstract class BaseRule extends CoRRuleBook<Boolean> {

    private UUID taskId;
    private Run run;

    public BaseRule(UUID taskId, Run run) {
        this.taskId = taskId;
        this.run = run;
        super.setDefaultResult(Boolean.FALSE);
    }

    public enum Run {
        ONCE,
        MULTI
    }
}
