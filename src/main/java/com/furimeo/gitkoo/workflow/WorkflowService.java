package com.furimeo.gitkoo.workflow;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

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

    /** Queue of runs waiting to be executed by a worker thread. */
    private final BlockingQueue<QueuedRun> queue = new LinkedBlockingQueue<>();
    /** Worker pool sized by {@code gitkoo.ci.workers} (DESIGN.md §37). */
    private ExecutorService workers;

    public WorkflowService(WorkflowRunRepository runRepository, WorkflowExecutor executor,
                          GitService gitService, GitKooProperties properties) {
        this.runRepository = runRepository;
        this.executor = executor;
        this.gitService = gitService;
        this.properties = properties;
    }

    /** Starts the worker threads that drain the run queue. */
    @PostConstruct
    void startWorkers() {
        int n = Math.max(1, properties.getCi().getWorkers());
        workers = Executors.newFixedThreadPool(n, r -> {
            Thread t = new Thread(r, "gitkoo-workflow-worker");
            t.setDaemon(true);
            return t;
        });
        for (int i = 0; i < n; i++) {
            workers.submit(this::workerLoop);
        }
        log.info("Started {} workflow worker(s)", n);
    }

    /** Stops the workers and drains the queue on shutdown. */
    @PreDestroy
    void stopWorkers() {
        if (workers == null) {
            return;
        }
        workers.shutdownNow();
        try {
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Workflow workers did not terminate cleanly within 5s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Worker loop: dequeue a run, execute it, and record the final status. */
    private void workerLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            QueuedRun job;
            try {
                job = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            runQueued(job);
        }
    }

    /** Executes a single dequeued run and persists its final status. */
    private void runQueued(QueuedRun job) {
        WorkflowRun run = job.run();
        try {
            run.setStatus(WorkflowRun.Status.RUNNING.name());
            run.setStartedAt(OffsetDateTime.now());
            run = runRepository.save(run);

            boolean success = executor.execute(run.getId(), job.workflow(), job.workspace(),
                    job.context(), job.secrets());
            run.setStatus(success ? WorkflowRun.Status.SUCCESS.name() : WorkflowRun.Status.FAILED.name());
        } catch (Exception e) {
            log.error("Workflow execution failed for run {}", run.getId(), e);
            run.setStatus(WorkflowRun.Status.FAILED.name());
        } finally {
            run.setFinishedAt(OffsetDateTime.now());
            runRepository.save(run);
        }
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

        // For MVP, use the repo storage path's parent as workspace (no separate checkout).
        Path workspace = Path.of(repo.getStoragePath()).getParent();

        // Enqueue for a worker thread; return immediately (DESIGN.md §31, §32, §63).
        // Secrets resolution is not wired in MVP, so none are passed.
        queue.add(new QueuedRun(run, workflow, workspace, context, Map.of()));
        return run;
    }

    /** Lists recent workflow runs for a repository. */
    public List<WorkflowRun> listRuns(Long repositoryId) {
        return runRepository.findByRepositoryIdOrderByIdDesc(repositoryId);
    }

    /** Loads a single workflow run by id. */
    public Optional<WorkflowRun> findRun(Long runId) {
        return runRepository.findById(runId);
    }

    /** Reads the persisted log output for a run (empty if none). */
    public String readLog(Long runId) {
        return executor.readLog(runId);
    }

    /** Parses a .koo source file into a Workflow AST. */
    public Workflow parse(String source) {
        List<Token> tokens = new WorkflowLexer(source).tokenize();
        Workflow workflow = new WorkflowParser(tokens).parse();
        WorkflowValidator.validate(workflow);
        return workflow;
    }

    /** A queued workflow run plus everything a worker needs to execute it. */
    private record QueuedRun(WorkflowRun run, Workflow workflow, Path workspace,
                            Map<String, String> context, Map<String, String> secrets) {
    }
}
