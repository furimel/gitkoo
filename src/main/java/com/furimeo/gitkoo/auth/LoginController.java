package com.furimeo.gitkoo.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Shows the login page. Spring Security handles the actual authentication at
 * {@code POST /login} (DESIGN.md §43).
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/")
    public String dashboard() {
        return "dashboard";
    }
}
