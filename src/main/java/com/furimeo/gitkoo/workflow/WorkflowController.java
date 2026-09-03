package com.furimeo.gitkoo.workflow;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.repository.Repository;
import com.furimeo.gitkoo.repository.RepositoryService;

/** Actions page showing workflow runs (DESIGN.md §12). */
@Controller
public class WorkflowController {

    private final WorkflowService workflowService;
    private final RepositoryService repositoryService;
    private final UserService userService;

    public WorkflowController(WorkflowService workflowService, RepositoryService repositoryService,
                             UserService userService) {
        this.workflowService = workflowService;
        this.repositoryService = repositoryService;
        this.userService = userService;
    }

    @GetMapping("/{username}/{name}/actions")
    public String actions(@PathVariable String username, @PathVariable String name, Model model) {
        Repository repo = resolveRepo(username, name);
        List<WorkflowRun> runs = workflowService.listRuns(repo.getId());
        model.addAttribute("title", "Actions \u00b7 " + username + "/" + name);
        model.addAttribute("owner", username);
        model.addAttribute("repo", repo);
        model.addAttribute("runs", runs);
        return "repository/actions";
    }

    /**
     * Streams the persisted log output for a single workflow run as plain text
     * (DESIGN.md §109 "workflow logs"). The run must belong to the resolved
     * repository, otherwise a 404 is returned.
     */
    @GetMapping(value = "/{username}/{name}/actions/{runId}/logs", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public ResponseEntity<String> logs(@PathVariable String username, @PathVariable String name,
                                       @PathVariable Long runId) {
        Repository repo = resolveRepo(username, name);
        return workflowService.findRun(runId)
                .filter(run -> run.getRepositoryId().equals(repo.getId()))
                .map(run -> ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN)
                        .body(workflowService.readLog(runId)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Repository resolveRepo(String username, String name) {
        var owner = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return repositoryService.findByOwnerAndName(Repository.OwnerType.USER.name(), owner.getId(), name)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found"));
    }
}
