package com.furimeo.gitkoo.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.furimeo.gitkoo.repository.Repository;

/**
 * Basic SQL-based search for repositories and users (DESIGN.md §48).
 *
 * <p>MVP uses database LIKE queries \u2014 no Elasticsearch, OpenSearch, or vector
 * search (DESIGN.md §48). This is sufficient for the expected instance scale
 * (5-100 users, 10-5000 repos, DESIGN.md §97).
 */
@Controller
public class SearchController {

    private final JdbcTemplate jdbc;

    public SearchController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("title", "Search");
        if (q == null || q.isBlank()) {
            model.addAttribute("results", List.of());
            return "search";
        }
        String pattern = "%" + q.toLowerCase() + "%";
        List<SearchResult> results = new ArrayList<>();
        // Search repositories by name.
        jdbc.query("SELECT id, owner_type, owner_id, name, description FROM repositories WHERE LOWER(name) LIKE ? LIMIT 20",
                rs -> {
                    results.add(new SearchResult(
                            "repository",
                            rs.getString("owner_type") + ":" + rs.getLong("owner_id"),
                            rs.getString("name"),
                            rs.getString("description")
                    ));
                },
                pattern);
        model.addAttribute("results", results);
        model.addAttribute("query", q);
        return "search";
    }

    /** A single search result entry. */
    public record SearchResult(String type, String owner, String name, String description) {}
}
