package com.furimeo.gitkoo.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.furimeo.gitkoo.workflow.ast.Stmt;
import com.furimeo.gitkoo.workflow.ast.Workflow;

/**
 * Unit tests for the workflow lexer/parser/validator (DESIGN.md §94, §117).
 */
class WorkflowParserTest {

    private Workflow parse(String source) {
        List<Token> tokens = new WorkflowLexer(source).tokenize();
        Workflow workflow = new WorkflowParser(tokens).parse();
        WorkflowValidator.validate(workflow);
        return workflow;
    }

    @Test
    void parsesSimpleWorkflow() {
        Workflow wf = parse("""
            workflow build
                on push main
                run ./gradlew test
            """);
        assertThat(wf.name()).isEqualTo("build");
        assertThat(wf.triggers()).hasSize(1);
        assertThat(wf.triggers().get(0).event()).isEqualTo("push");
        assertThat(wf.triggers().get(0).filter()).isEqualTo("main");
        assertThat(wf.body()).hasSize(1);
        assertThat(wf.body().get(0)).isInstanceOf(Stmt.Run.class);
        assertThat(((Stmt.Run) wf.body().get(0)).command()).isEqualTo("./gradlew test");
    }

    @Test
    void parsesMultipleTriggers() {
        Workflow wf = parse("""
            workflow test
                on push main
                on tag
                on pull_request
                run ./gradlew test
            """);
        assertThat(wf.triggers()).hasSize(3);
    }

    @Test
    void parsesEnvAndSecret() {
        Workflow wf = parse("""
            workflow build
                on push
                env JAVA_HOME=/opt/java
                secret DEPLOY_TOKEN
                run ./gradlew build
            """);
        assertThat(wf.envs()).hasSize(1);
        assertThat(wf.envs().get(0).name()).isEqualTo("JAVA_HOME");
        assertThat(wf.envs().get(0).value()).isEqualTo("/opt/java");
        assertThat(wf.secrets()).hasSize(1);
        assertThat(wf.secrets().get(0).name()).isEqualTo("DEPLOY_TOKEN");
    }

    @Test
    void parsesArtifact() {
        Workflow wf = parse("""
            workflow build
                on push
                run ./gradlew build
                artifact build/libs/*.jar
            """);
        assertThat(wf.body()).hasSize(2);
        assertThat(wf.body().get(1)).isInstanceOf(Stmt.Artifact.class);
        assertThat(((Stmt.Artifact) wf.body().get(1)).glob()).isEqualTo("build/libs/*.jar");
    }

    @Test
    void parsesParallel() {
        Workflow wf = parse("""
            workflow test
                on push
                parallel
                    run ./test-java
                    run ./test-cpp
                end
            """);
        assertThat(wf.body()).hasSize(1);
        assertThat(wf.body().get(0)).isInstanceOf(Stmt.Parallel.class);
        assertThat(((Stmt.Parallel) wf.body().get(0)).body()).hasSize(2);
    }

    @Test
    void parsesIf() {
        Workflow wf = parse("""
            workflow build
                on push
                if branch == "release"
                    run ./publish
                end
            """);
        assertThat(wf.body()).hasSize(1);
        assertThat(wf.body().get(0)).isInstanceOf(Stmt.If.class);
        Stmt.If ifStmt = (Stmt.If) wf.body().get(0);
        assertThat(ifStmt.expr().variable()).isEqualTo("branch");
        assertThat(ifStmt.expr().operator()).isEqualTo("==");
        assertThat(ifStmt.expr().literal()).isEqualTo("release");
    }

    @Test
    void parsesIfElse() {
        Workflow wf = parse("""
            workflow build
                on push
                if branch != "main"
                    run ./dev-build
                else
                    run ./prod-build
                end
            """);
        Stmt.If ifStmt = (Stmt.If) wf.body().get(0);
        assertThat(ifStmt.thenBody()).hasSize(1);
        assertThat(ifStmt.elseBody()).hasSize(1);
    }

    @Test
    void parsesTimeout() {
        Workflow wf = parse("""
            workflow build
                on push
                timeout 30m
                run ./long-build
            """);
        assertThat(wf.body()).hasSize(2);
        assertThat(wf.body().get(0)).isInstanceOf(Stmt.Timeout.class);
        assertThat(((Stmt.Timeout) wf.body().get(0)).duration()).isEqualTo("30m");
    }

    @Test
    void parsesRunShell() {
        Workflow wf = parse("""
            workflow build
                on push
                run shell "echo $HOME && ./build"
            """);
        Stmt.Run run = (Stmt.Run) wf.body().get(0);
        assertThat(run.shell()).isTrue();
        assertThat(run.command()).isEqualTo("echo $HOME && ./build");
    }

    @Test
    void parsesManualTrigger() {
        Workflow wf = parse("""
            workflow deploy
                on manual
                run ./deploy.sh
            """);
        assertThat(wf.triggers().get(0).event()).isEqualTo("manual");
    }

    @Test
    void rejectsMissingTrigger() {
        assertThatThrownBy(() -> parse("""
            workflow build
                run ./gradlew test
            """)).isInstanceOf(WorkflowParseException.class)
                .hasMessageContaining("no trigger");
    }

    @Test
    void rejectsNoRunStatement() {
        assertThatThrownBy(() -> parse("""
            workflow build
                on push
                env FOO=bar
            """)).isInstanceOf(WorkflowParseException.class)
                .hasMessageContaining("no run statement");
    }

    @Test
    void rejectsUnknownCommandWithSuggestion() {
        assertThatThrownBy(() -> parse("""
            workflow build
                on push
                artifcat build/libs/*.jar
            """)).isInstanceOf(WorkflowParseException.class)
                .hasMessageContaining("unknown command")
                .hasMessageContaining("Did you mean: artifact");
    }

    @Test
    void ignoresComments() {
        Workflow wf = parse("""
            # This is a comment
            workflow build
                on push main
                # inline comment
                run ./gradlew test
            """);
        assertThat(wf.name()).isEqualTo("build");
        assertThat(wf.body()).hasSize(1);
    }
}
