package io.student.rangiffler.jupiter.extension;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class ProbeTargetCondition implements ExecutionCondition {
    public static final String PARAMETER = "rfr.probe";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return context.getConfigurationParameter(PARAMETER, Boolean::parseBoolean).orElse(false)
                ? ConditionEvaluationResult.enabled("Probe target runs inside EngineTestKit")
                : ConditionEvaluationResult.disabled("Probe target runs only inside EngineTestKit");
    }
}
