package com.furimeo.gitkoo.web;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Renders errors through the same Inertia pipeline as every other page.
 *
 * <p>Spring Boot's default error handling looks for a template; with no template
 * engine it falls back to the whitelabel page, which is the one screen in the
 * product that would not look like the product. This hands the status to the client
 * as props instead, so a 404 is styled by the same React layout as everything else.
 *
 * <p>Only the status reaches the browser. The exception message and stack trace stay
 * in the server log: a message like "Repository not found: minh/secret" would confirm
 * to a stranger that the repository exists.
 */
@Controller
public class ErrorPageController implements ErrorController {

    @RequestMapping("/error")
    public String error(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        model.addAttribute("status", status instanceof Integer code ? code : 500);
        return "error";
    }
}
