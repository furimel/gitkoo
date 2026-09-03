package com.furimeo.gitkoo.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.furimeo.gitkoo.activity.AuditEvent;
import com.furimeo.gitkoo.activity.AuditService;
import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserRepository;
import com.furimeo.gitkoo.config.GitKooProperties;

/**
 * Admin dashboard: users, system info, audit log (DESIGN.md §99, §100).
 *
 * <p>Restricted to ROLE_ADMIN via Spring Security configuration.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final GitKooProperties properties;

    public AdminController(UserRepository userRepository, AuditService auditService,
                          GitKooProperties properties) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.properties = properties;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("title", "Admin");
        long userCount = userRepository.count();
        model.addAttribute("userCount", userCount);
        return "admin/index";
    }

    @GetMapping("/users")
    public String users(Model model) {
        List<User> users = new java.util.ArrayList<>();
        userRepository.findAll().forEach(users::add);
        model.addAttribute("title", "Users \u00b7 Admin");
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/system")
    public String system(Model model) {
        model.addAttribute("title", "System \u00b7 Admin");
        model.addAttribute("gitkooVersion", "0.0.1-SNAPSHOT");
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        model.addAttribute("gitBinary", properties.getGit().getBinary());
        model.addAttribute("dataPath", properties.getData());
        model.addAttribute("ciEnabled", properties.getCi().isEnabled());
        model.addAttribute("ciWorkers", properties.getCi().getWorkers());
        model.addAttribute("sshEnabled", properties.getSsh().isEnabled());
        model.addAttribute("sshPort", properties.getSsh().getPort());
        return "admin/system";
    }

    @GetMapping("/audit")
    public String audit(Model model) {
        List<AuditEvent> events = auditService.listRecent();
        model.addAttribute("title", "Audit log \u00b7 Admin");
        model.addAttribute("events", events);
        return "admin/audit";
    }
}
