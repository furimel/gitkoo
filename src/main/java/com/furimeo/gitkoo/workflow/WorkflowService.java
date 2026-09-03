package com.furimeo.gitkoo.workflow;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.furimeo.gitkoo.config.GitKooProperties;
import com.furimeo.gitkoo.git.GitService;
import com.furimeo.gitkoo.repository.Repository;
import com.furimeo.gitkoo.workflow.ast.Workflow;

/**
 * Triggers and runs workflows on Git events (DESIGN.md §27, §31, §82).
 *
 * <p>MVP supports push, tag, pull_request, and manual triggers. On trigger, the
 * workflow is parsed, validated, and executed in a workspace checkout.
 */
@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    private final WorkflowRunRepository runRepository;
    private final WorkflowExecutor executor;
    private final GitService gitService;
    private final GitKooProperties properties;

    public WorkflowService(WorkflowRunRepository runRepository, WorkflowExecutor executor,
                          GitService gitService, GitKooProperties properties) {
        this.runRepository = runRepository;
        this.executor = executor;
        this.gitService = gitService;
        this.properties = properties;
    }

    /**
     * Triggers a workflow run for a repository event.
     *
     * @param workflow   the parsed workflow AST
     * @param repo       the repository
     * @param event      event type (push/tag/pull_request/manual)
     * @param ref        the Git ref (e.g. "refs/heads/main")
     * @param commitSha  the commit SHA
     * @param triggeredByUserId  the user who triggered (null for push)
     * @return the created WorkflowRun
     */
    public WorkflowRun trigger(Workflow workflow, Repository repo, String event,
                               String ref, String commitSha, Long triggeredByUserId) {
        // Create the run record.
        WorkflowRun run = new WorkflowRun();
        run.setWorkflowId(0L); // MVP: workflow registry not yet wired
        run.setRepositoryId(repo.getId());
        run.setCommitSha(commitSha);
        run.setEvent(event);
        run.setRef(ref);
        run.setStatus(WorkflowRun.Status.QUEUED.name());
        run.setTriggeredByUserId(triggeredByUserId);
        run = runRepository.save(run);

        // Build context.
        Map<String, String> context = new HashMap<>();
        context.put("GITKOO_REPOSITORY", repo.getName());
        context.put("GITKOO_COMMIT", commitSha);
        context.put("GITKOO_BRANCH", ref != null ? ref.replace("refs/heads/", "") : "");
        context.put("GITKOO_EVENT", event);
        context.put("GITKOO_REF", ref != null ? ref : "");
        context.put("GITKOO_RUN_ID", String.valueOf(run.getId()));

        // Execute.
        run.setStatus(WorkflowRun.Status.RUNNING.name());
        run.setStartedAt(OffsetDateTime.now());
        run = runRepository.save(run);

        // MVP: run synchronously in the caller thread. A queue + worker thread
        // can be added later (DESIGN.md §31, §32, §63).
        boolean success = false;
        try {
            // For MVP, use the repo storage path as workspace (no separate checkout).
            Path workspace = Path.of(repo.getStoragePath()).getParent();
            success = executor.execute(workflow, workspace, context, Map.of());
        } catch (Exception e) {
            log.error("Workflow execution failed", e);
        } finally {
            run.setStatus(success ? WorkflowRun.Status.SUCCESS.name() : WorkflowRun.Status.FAILED.name());
            run.setFinishedAt(OffsetDateTime.now());
            runRepository.save(run);
        }
        return run;
    }

    /** Lists recent workflow runs for a repository. */
    public List<WorkflowRun> listRuns(Long repositoryId) {
        return runRepository.findByRepositoryIdOrderByIdDesc(repositoryId);
    }

    /** Parses a .koo source file into a Workflow AST. */
    public Workflow parse(String source) {
        List<Token> tokens = new WorkflowLexer(source).tokenize();
        Workflow workflow = new WorkflowParser(tokens).parse();
        WorkflowValidator.validate(workflow);
        return workflow;
    }
}
