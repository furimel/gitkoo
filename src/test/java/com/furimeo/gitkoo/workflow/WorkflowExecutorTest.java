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
}
