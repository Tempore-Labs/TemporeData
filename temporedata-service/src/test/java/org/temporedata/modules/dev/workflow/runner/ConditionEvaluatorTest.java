package org.temporedata.modules.dev.workflow.runner;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionEvaluatorTest {

    private final ConditionEvaluator evaluator = new ConditionEvaluator();

    private Map<String, NodeRunResult> upstream(long rows, boolean success, String id) {
        Map<String, NodeRunResult> m = new LinkedHashMap<>();
        m.put(id, success ? NodeRunResult.ok("ok", rows) : NodeRunResult.failed("failed"));
        return m;
    }

    @Test
    void nullOrBlankExprDefaultsTrue() {
        assertThat(evaluator.evaluate(null, null)).isTrue();
        assertThat(evaluator.evaluate("", null)).isTrue();
        assertThat(evaluator.evaluate("   ", null)).isTrue();
    }

    @Test
    void numericRowsComparison() {
        Map<String, NodeRunResult> up = upstream(120, true, "n1");
        assertThat(evaluator.evaluate("node.n1.rows > 100", up)).isTrue();
        assertThat(evaluator.evaluate("node.n1.rows >= 120", up)).isTrue();
        assertThat(evaluator.evaluate("node.n1.rows != 0", up)).isTrue();
        assertThat(evaluator.evaluate("node.n1.rows < 100", up)).isFalse();
    }

    @Test
    void successAndStatus() {
        Map<String, NodeRunResult> up = upstream(10, true, "a");
        assertThat(evaluator.evaluate("node.a.success", up)).isTrue();
        assertThat(evaluator.evaluate("!node.a.success", up)).isFalse();
        assertThat(evaluator.evaluate("node.a.status == 'SUCCESS'", up)).isTrue();
        Map<String, NodeRunResult> failed = upstream(0, false, "b");
        assertThat(evaluator.evaluate("node.b.status == 'FAILED'", failed)).isTrue();
    }

    @Test
    void booleanLogicAndParentheses() {
        Map<String, NodeRunResult> up = upstream(50, true, "x");
        assertThat(evaluator.evaluate("node.x.rows > 10 && node.x.success", up)).isTrue();
        assertThat(evaluator.evaluate("(node.x.rows > 100) || (node.x.success)", up)).isTrue();
        assertThat(evaluator.evaluate("(node.x.rows > 100) && (node.x.success)", up)).isFalse();
    }

    @Test
    void arithmetic() {
        Map<String, NodeRunResult> up = upstream(20, true, "y");
        assertThat(evaluator.evaluate("node.y.rows + 30 >= 50", up)).isTrue();
    }
}