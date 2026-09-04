package com.furimeo.gitkoo.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class WorkflowExecutorTest {

    @Test
    void shlexSplitHandlesSimpleCommand() {
        List<String> tokens = WorkflowExecutor.shlexSplit("./gradlew test");
        assertThat(tokens).containsExactly("./gradlew", "test");
    }

    @Test
    void shlexSplitHandlesQuotedArgs() {
        List<String> tokens = WorkflowExecutor.shlexSplit("echo \"hello world\" foo");
        assertThat(tokens).containsExactly("echo", "hello world", "foo");
    }

    @Test
    void shlexSplitHandlesEmpty() {
        assertThat(WorkflowExecutor.shlexSplit("")).isEmpty();
    }

    @Test
    void shlexSplitHandlesMultipleSpaces() {
        List<String> tokens = WorkflowExecutor.shlexSplit("git   commit   -m   \"wip\"");
        assertThat(tokens).containsExactly("git", "commit", "-m", "wip");
    }

    @Test
    void maskSecretsReplacesSecretValue() {
        String masked = WorkflowExecutor.maskSecrets(
                "token is supersecret123", List.of("supersecret123"));
        assertThat(masked).isEqualTo("token is ***");
    }

    @Test
    void maskSecretsReplacesMultipleOccurrences() {
        String masked = WorkflowExecutor.maskSecrets(
                "pw=abc pw=abc", List.of("abc"));
        assertThat(masked).isEqualTo("pw=*** pw=***");
    }

    @Test
    void maskSecretsLeavesNonSecretTextUnchanged() {
        String masked = WorkflowExecutor.maskSecrets("no secrets here", List.of("abc"));
        assertThat(masked).isEqualTo("no secrets here");
    }

    @Test
    void maskSecretsHandlesNoSecrets() {
        assertThat(WorkflowExecutor.maskSecrets("anything", List.of()))
                .isEqualTo("anything");
    }

    @Test
    void maskSecretsHandlesNullAndEmpty() {
        assertThat(WorkflowExecutor.maskSecrets(null, List.of("x"))).isNull();
        assertThat(WorkflowExecutor.maskSecrets("x", null)).isEqualTo("x");
        // A list may contain empty/null entries; those are skipped, not blown up on.
        var secrets = new java.util.ArrayList<String>();
        secrets.add("");
        secrets.add(null);
        assertThat(WorkflowExecutor.maskSecrets("x", secrets)).isEqualTo("x");
    }
}
