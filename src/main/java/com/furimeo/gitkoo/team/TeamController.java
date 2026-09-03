package com.furimeo.gitkoo.team;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;

/**
 * Team creation and viewing (DESIGN.md §21).
 */
@Controller
@RequestMapping("/teams")
public class TeamController {

    private final TeamService teamService;
    private final UserService userService;

    public TeamController(TeamService teamService, UserService userService) {
        this.teamService = teamService;
        this.userService = userService;
    }

    @GetMapping("/new")
    public String newTeamForm(Model model) {
        model.addAttribute("title", "New team");
        return "team/new";
    }

    @PostMapping("/new")
    public String createTeam(@RequestParam String name, @RequestParam(required = false) String displayName,
                            @RequestParam(required = false) String description,
                            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                            RedirectAttributes ra) {
        User creator = userService.findByUsername(principal.getUsername()).orElseThrow();
        try {
            Team team = teamService.create(name, displayName, description, creator.getId());
            return "redirect:/teams/" + team.getName();
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/teams/new";
        }
    }

    @GetMapping("/{name}")
    public String viewTeam(@PathVariable String name, Model model) {
        Team team = teamService.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + name));
        List<TeamMember> members = teamService.listMembers(team.getId());
        model.addAttribute("title", "Team \u00b7 " + name);
        model.addAttribute("team", team);
        model.addAttribute("members", members);
        model.addAttribute("users", members.stream()
                .map(m -> userService.findById(m.getUserId()).orElse(null))
                .toList());
        return "team/view";
    }

    @PostMapping("/{name}/members")
    public String addMember(@PathVariable String name, @RequestParam String username,
                           @RequestParam(defaultValue = "MEMBER") String role,
                           @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                           RedirectAttributes ra) {
        Team team = teamService.findByName(name).orElseThrow();
        User toAdd = userService.findByUsername(username).orElse(null);
        if (toAdd == null) {
            ra.addFlashAttribute("error", "User not found: " + username);
            return "redirect:/teams/" + name;
        }
        try {
            teamService.addMember(team.getId(), toAdd.getId(), role);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teams/" + name;
    }
}
