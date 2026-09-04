package com.furimeo.gitkoo.issue;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.repository.Repository;
import com.furimeo.gitkoo.repository.RepositoryService;

/**
 * Wires labels into the issue UI: adding/removing labels on an issue and creating
 * new labels for a repository (DESIGN.md §18).
 *
 * <p>The issue view template renders existing labels directly via the
 * {@code LabelService} Spring bean; this controller owns the POST actions that mutate
 * label state. Repositories and issues are resolved the same way as
 * {@link IssueController} (owner username + repo name), reusing
 * {@link IssueService} and {@link RepositoryService} without modifying them.
 */
@Controller
@RequestMapping("/{username}/{name}")
public class LabelController {

    private final LabelService labelService;
    private final LabelRepository labelRepository;
    private final IssueService issueService;
    private final RepositoryService repositoryService;
    private final UserService userService;

    public LabelController(LabelService labelService, LabelRepository labelRepository,
                           IssueService issueService, RepositoryService repositoryService,
                           UserService userService) {
        this.labelService = labelService;
        this.labelRepository = labelRepository;
        this.issueService = issueService;
        this.repositoryService = repositoryService;
        this.userService = userService;
    }

    /** Adds a label (looked up by name) to an issue, then returns to the issue view. */
    @PostMapping("/issues/{number}/labels/add")
    public String addLabel(@PathVariable String username, @PathVariable String name,
                           @PathVariable Integer number, @RequestParam String labelName,
                           RedirectAttributes ra) {
        Repository repo = resolveRepo(username, name);
        Label label = labelRepository.findByRepositoryIdAndName(repo.getId(), labelName)
                .orElseThrow(() -> new IllegalArgumentException("Label not found: " + labelName));
        Issue issue = issueService.findByRepositoryAndNumber(repo.getId(), number);
        if (issue == null) {
            throw new IllegalArgumentException("Issue not found: #" + number);
        }
        labelService.addLabelToIssue(issue.getId(), label.getId());
        ra.addFlashAttribute("labelMessage", "Added label '" + label.getName() + "'");
        return "redirect:/" + username + "/" + name + "/issues/" + number;
    }

    /** Removes a label (by id) from an issue, then returns to the issue view. */
    @PostMapping("/issues/{number}/labels/remove")
    public String removeLabel(@PathVariable String username, @PathVariable String name,
                              @PathVariable Integer number, @RequestParam Long labelId,
                              RedirectAttributes ra) {
        Repository repo = resolveRepo(username, name);
        Issue issue = issueService.findByRepositoryAndNumber(repo.getId(), number);
        if (issue == null) {
            throw new IllegalArgumentException("Issue not found: #" + number);
        }
        labelService.removeLabelFromIssue(issue.getId(), labelId);
        ra.addFlashAttribute("labelMessage", "Removed label");
        return "redirect:/" + username + "/" + name + "/issues/" + number;
    }

    /**
     * Creates a new label for a repository. After creating, returns to the issue the
     * caller came from when {@code returnNumber} is supplied, otherwise to the issues
     * list.
     */
    @PostMapping("/labels/new")
    public String createLabel(@PathVariable String username, @PathVariable String name,
                             @RequestParam("name") String labelName, @RequestParam String color,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) Integer returnNumber,
                             RedirectAttributes ra) {
        Repository repo = resolveRepo(username, name);
        labelService.createLabel(repo.getId(), labelName, color, description);
        ra.addFlashAttribute("labelMessage", "Created label '" + labelName + "'");
        if (returnNumber != null) {
            return "redirect:/" + username + "/" + name + "/issues/" + returnNumber;
        }
        return "redirect:/" + username + "/" + name + "/issues";
    }

    private Repository resolveRepo(String username, String name) {
        User owner = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return repositoryService.findByOwnerAndName(Repository.OwnerType.USER.name(), owner.getId(), name)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found"));
    }
}
