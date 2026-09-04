package com.furimeo.gitkoo.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import com.furimeo.gitkoo.config.GitKooProperties;
import com.furimeo.gitkoo.workflow.ast.Stmt;
import com.furimeo.gitkoo.workflow.ast.Workflow;

/**
 * Unit tests for artifact collection in {@link WorkflowExecutor} (DESIGN.md §109).
 *
 * <p>Stands up a {@link WorkflowExecutor} with a temp data directory and a mocked
 * {@link WorkflowArtifactRepository}, then drives a workflow whose only body
 * statement is an {@code artifact <glob>} and asserts that matching files are
 * copied and recorded while non-matching files are ignored.
 */
class WorkflowExecutorArtifactTest {

    private static Workflow artifactWorkflow(String glob) {
        return new Workflow("test", List.of(), List.of(), List.of(),
                List.of(new Stmt.Artifact(glob)));
    }

    @Test
    void collectsMatchingFilesToArtifactsDirAndRecordsRow(@TempDir Path data) throws Exception {
        GitKooProperties props = new GitKooProperties();
        props.setData(data.toString());
        props.getCi().setWorkers(1);
        WorkflowArtifactRepository repo = mock(WorkflowArtifactRepository.class);
        when(repo.save(any(WorkflowArtifact.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowExecutor executor = new WorkflowExecutor(props, null, repo);

        Path workspace = Files.createDirectories(data.resolve("workspace"));
        Path jarDir = Files.createDirectories(workspace.resolve("build/libs"));
        Path jar = Files.writeString(jarDir.resolve("app.jar"), "jar-bytes");
        long jarSize = Files.size(jar);
        Files.writeString(jarDir.resolve("notes.txt"), "ignored");

        boolean ok = executor.execute(7L, artifactWorkflow("build/libs/*.jar"),
                workspace, Map.of(), Map.of());

        assertThat(ok).isTrue();

        Path copied = data.resolve("artifacts/7/build/libs/app.jar");
        assertThat(Files.exists(copied)).as("matching jar copied to artifact store").isTrue();
        assertThat(Files.readString(copied)).isEqualTo("jar-bytes");
        assertThat(Files.exists(data.resolve("artifacts/7/build/libs/notes.txt")))
                .as("non-matching file is not copied").isFalse();

        ArgumentCaptor<WorkflowArtifact> captor = ArgumentCaptor.forClass(WorkflowArtifact.class);
        verify(repo, atLeastOnce()).save(captor.capture());
        WorkflowArtifact saved = captor.getValue();
        assertThat(saved.getRunId()).isEqualTo(7L);
        assertThat(saved.getName()).isEqualTo("app.jar");
        assertThat(saved.getSize()).isEqualTo(jarSize);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getFilePath()).isNotNull();
    }

    @Test
    void collectingWithNoMatchesIsNotAFailure(@TempDir Path data) throws Exception {
        GitKooProperties props = new GitKooProperties();
        props.setData(data.toString());
        props.getCi().setWorkers(1);
        WorkflowArtifactRepository repo = mock(WorkflowArtifactRepository.class);
        when(repo.save(any(WorkflowArtifact.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowExecutor executor = new WorkflowExecutor(props, null, repo);

        Path workspace = Files.createDirectories(data.resolve("workspace"));
        Files.createDirectories(workspace.resolve("build/libs"));

        boolean ok = executor.execute(9L, artifactWorkflow("build/libs/*.jar"),
                workspace, Map.of(), Map.of());

        assertThat(ok).as("zero matches is not a run failure").isTrue();
        verify(repo, never()).save(any(WorkflowArtifact.class));
    }

    @Test
    void starStarGlobMatchesNestedFiles(@TempDir Path data) throws Exception {
        GitKooProperties props = new GitKooProperties();
        props.setData(data.toString());
        props.getCi().setWorkers(1);
        WorkflowArtifactRepository repo = mock(WorkflowArtifactRepository.class);
        when(repo.save(any(WorkflowArtifact.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowExecutor executor = new WorkflowExecutor(props, null, repo);

        Path workspace = Files.createDirectories(data.resolve("workspace"));
        Files.createDirectories(workspace.resolve("out/sub"));
        Path nested = Files.writeString(workspace.resolve("out/sub/lib.jar"), "nested-jar");
        long size = Files.size(nested);

        boolean ok = executor.execute(11L, artifactWorkflow("**/*.jar"),
                workspace, Map.of(), Map.of());

        assertThat(ok).isTrue();
        assertThat(Files.exists(data.resolve("artifacts/11/out/sub/lib.jar"))).isTrue();

        ArgumentCaptor<WorkflowArtifact> captor = ArgumentCaptor.forClass(WorkflowArtifact.class);
        verify(repo, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("lib.jar");
        assertThat(captor.getValue().getSize()).isEqualTo(size);
        assertThat(captor.getValue().getRunId()).isEqualTo(11L);
    }
}
