/* GitKoo client script - vanilla JS, no build step.
 *
 * Only handles what HTMX is a poor fit for (docs/stack.md):
 *   - theme switching across Primer's colour modes
 *   - clipboard copy for clone URLs
 *   - relative timestamps ("3 hours ago")
 *   - confirm dialogs for destructive actions
 *
 * Theming uses Primer's own attributes on <html>:
 *   data-color-mode="auto"  + data-light-theme / data-dark-theme  -> follow the OS
 *   data-color-mode="light" + data-light-theme="<theme>"          -> pinned theme
 * The initial values are applied inline in <head> so the page never flashes the
 * wrong palette; this file only cycles them afterwards.
 *
 * Loaded once via the layout fragment; safe on every page.
 */
(function () {
  "use strict";

  var STORAGE_KEY = "gitkoo-theme";

  /* Cycle order matches GitHub's own appearance options. */
  var THEMES = ["auto", "light", "dark", "dark_dimmed"];

  /* ── Theme ─────────────────────────────────────────────────────────── */

  function storedTheme() {
    try {
      var t = localStorage.getItem(STORAGE_KEY);
      return THEMES.indexOf(t) === -1 ? "auto" : t;
    } catch (e) {
      return "auto";
    }
  }

  function applyTheme(pref) {
    var el = document.documentElement;
    if (pref === "auto") {
      el.setAttribute("data-color-mode", "auto");
      el.setAttribute("data-light-theme", "light");
      el.setAttribute("data-dark-theme", "dark");
    } else {
      el.setAttribute("data-color-mode", "light");
      el.setAttribute("data-light-theme", pref);
    }
    try { localStorage.setItem(STORAGE_KEY, pref); } catch (e) { /* private mode */ }
    syncToggleIcons();
  }

  /* True when the page is currently painting a dark palette, whether that came
     from an explicit choice or from "auto" plus a dark OS setting. */
  function isDark() {
    var pref = storedTheme();
    if (pref === "dark" || pref === "dark_dimmed") { return true; }
    if (pref !== "auto") { return false; }
    return !!(window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches);
  }

  /* Show the icon for the theme you would switch TO, like GitHub does. */
  function syncToggleIcons() {
    var dark = isDark();
    document.querySelectorAll(".theme-icon-light").forEach(function (el) {
      el.style.display = dark ? "" : "none";
    });
    document.querySelectorAll(".theme-icon-dark").forEach(function (el) {
      el.style.display = dark ? "none" : "";
    });
    document.querySelectorAll(".theme-toggle").forEach(function (el) {
      el.setAttribute("title", "Theme: " + storedTheme().replace("_", " "));
    });
  }

  function cycleTheme() {
    var next = THEMES[(THEMES.indexOf(storedTheme()) + 1) % THEMES.length];
    applyTheme(next);
  }

  /* ── Clipboard ─────────────────────────────────────────────────────── */

  function copyText(text) {
    if (navigator.clipboard && window.isSecureContext) {
      return navigator.clipboard.writeText(text).catch(function () { fallbackCopy(text); });
    }
    return fallbackCopy(text);
  }

  function fallbackCopy(text) {
    var ta = document.createElement("textarea");
    ta.value = text;
    ta.setAttribute("readonly", "");
    ta.style.position = "fixed";
    ta.style.opacity = "0";
    document.body.appendChild(ta);
    ta.select();
    try { document.execCommand("copy"); } catch (e) { /* nothing else to try */ }
    document.body.removeChild(ta);
  }

  /* Swap the icon to a check for a moment, the way GitHub's copy button does. */
  function flashCopied(btn) {
    var use = btn.querySelector("use");
    if (!use) { return; }
    var original = use.getAttribute("href");
    use.setAttribute("href", "#octicon-check");
    btn.classList.add("color-fg-success");
    setTimeout(function () {
      use.setAttribute("href", original);
      btn.classList.remove("color-fg-success");
    }, 1600);
  }

  /* ── Relative time ─────────────────────────────────────────────────── */

  var UNITS = [
    ["year", 31536000],
    ["month", 2592000],
    ["week", 604800],
    ["day", 86400],
    ["hour", 3600],
    ["minute", 60]
  ];

  function relativeTime(iso) {
    var then = Date.parse(iso);
    if (isNaN(then)) { return null; }
    var seconds = Math.round((Date.now() - then) / 1000);
    if (seconds < 45) { return "just now"; }
    for (var i = 0; i < UNITS.length; i++) {
      var name = UNITS[i][0];
      var size = UNITS[i][1];
      if (seconds >= size) {
        var n = Math.floor(seconds / size);
        return n + " " + name + (n === 1 ? "" : "s") + " ago";
      }
    }
    return "just now";
  }

  /* <time datetime="..."> is rewritten in place; the machine-readable value stays
     in the attribute and the exact timestamp moves to the tooltip. */
  function applyRelativeTimes(root) {
    (root || document).querySelectorAll("time[datetime]").forEach(function (el) {
      var text = relativeTime(el.getAttribute("datetime"));
      if (!text) { return; }
      if (!el.title) { el.title = el.textContent.trim(); }
      el.textContent = text;
    });
  }

  /* ── Syntax highlighting ───────────────────────────────────────────── */

  /*
   * Both the blob viewer and the diff render one <td> per line, but highlighting
   * has to see the whole file at once or a block comment, a template literal or a
   * multi-line string ends on the line it started on. So: highlight the joined
   * text, then cut the result back into lines, re-opening whatever spans were
   * still open at each line break.
   *
   * highlight.js output only ever contains <span class="..."> and </span> around
   * escaped text, which is what makes this scan safe.
   */
  function splitHighlightedLines(html) {
    var lines = html.split("\n");
    var open = [];
    var out = [];
    for (var i = 0; i < lines.length; i++) {
      var prefix = open.join("");
      var tags = /<span [^>]*>|<\/span>/g;
      var match;
      while ((match = tags.exec(lines[i])) !== null) {
        if (match[0] === "</span>") {
          open.pop();
        } else {
          open.push(match[0]);
        }
      }
      out.push(prefix + lines[i] + new Array(open.length + 1).join("</span>"));
    }
    return out;
  }

  function languageAvailable(language) {
    return !!(language && window.hljs && window.hljs.getLanguage(language));
  }

  /* The blob viewer: one table, one language, full-file context. */
  function highlightBlob(table) {
    var language = table.getAttribute("data-language");
    if (!languageAvailable(language)) {
      return;
    }
    var cells = table.querySelectorAll(".blob-code");
    if (!cells.length) {
      return;
    }
    var source = Array.prototype.map.call(cells, function (cell) {
      return cell.textContent;
    }).join("\n");

    var highlighted = window.hljs.highlight(source, {
      language: language,
      ignoreIllegals: true
    }).value;
    var lines = splitHighlightedLines(highlighted);
    if (lines.length !== cells.length) {
      // Line counts disagreeing means the split is unsafe; plain text is correct.
      return;
    }
    for (var i = 0; i < cells.length; i++) {
      cells[i].innerHTML = lines[i];
    }
    table.classList.add("hljs-ready");
  }

  /*
   * A diff shows the two sides interleaved, so neither side is contiguous text.
   * Each side is reassembled separately, highlighted as a whole, then written back
   * to the rows it came from. That keeps multi-line constructs intact on both
   * sides, which per-line highlighting cannot do.
   */
  function highlightDiff(table) {
    var language = table.getAttribute("data-language");
    if (!languageAvailable(language)) {
      return;
    }
    var rows = table.querySelectorAll("tr");
    var sides = {old: [], new: []};

    Array.prototype.forEach.call(rows, function (row) {
      var cell = row.querySelector(".diff-code");
      if (!cell || row.classList.contains("diff-line--hunk")) {
        return;
      }
      // Context lines belong to both sides; each change belongs to one.
      if (!row.classList.contains("diff-line--add")) {
        sides.old.push(cell);
      }
      if (!row.classList.contains("diff-line--del")) {
        sides.new.push(cell);
      }
    });

    Object.keys(sides).forEach(function (side) {
      var cells = sides[side];
      if (!cells.length) {
        return;
      }
      var source = cells.map(function (cell) { return cell.textContent; }).join("\n");
      var lines = splitHighlightedLines(window.hljs.highlight(source, {
        language: language,
        ignoreIllegals: true
      }).value);
      if (lines.length !== cells.length) {
        return;
      }
      cells.forEach(function (cell, i) {
        // A context line is written twice, once per side; the result is identical.
        cell.innerHTML = lines[i];
      });
    });
    table.classList.add("hljs-ready");
  }

  /* Fenced code blocks in rendered Markdown: commonmark tags them language-*. */
  function highlightMarkdownBlocks(scope) {
    scope.querySelectorAll(".markdown-body pre > code[class*='language-']").forEach(function (block) {
      var language = (block.className.match(/language-([\w-]+)/) || [])[1];
      if (!languageAvailable(language) || block.dataset.highlighted) {
        return;
      }
      block.innerHTML = window.hljs.highlight(block.textContent, {
        language: language,
        ignoreIllegals: true
      }).value;
      block.dataset.highlighted = "true";
    });
  }

  function highlightAll(root) {
    if (!window.hljs) {
      return;
    }
    var scope = root || document;
    scope.querySelectorAll(".blob-table[data-language]").forEach(highlightBlob);
    scope.querySelectorAll(".diff-table[data-language]").forEach(highlightDiff);
    highlightMarkdownBlocks(scope);
  }

  /* ── Wiring ────────────────────────────────────────────────────────── */

  document.addEventListener("DOMContentLoaded", function () {
    syncToggleIcons();
    applyRelativeTimes(document);
    highlightAll(document);

    /* Repaint the toggle when the OS flips while we are on "auto". */
    if (window.matchMedia) {
      var query = window.matchMedia("(prefers-color-scheme: dark)");
      var onChange = function () { if (storedTheme() === "auto") { syncToggleIcons(); } };
      if (query.addEventListener) {
        query.addEventListener("change", onChange);
      } else if (query.addListener) {
        query.addListener(onChange);
      }
    }

    document.addEventListener("click", function (event) {
      if (event.target.closest(".theme-toggle")) {
        cycleTheme();
        return;
      }

      var copyBtn = event.target.closest("[data-copy]");
      if (copyBtn) {
        copyText(copyBtn.getAttribute("data-copy"));
        flashCopied(copyBtn);
        return;
      }

      var copyInput = event.target.closest("[data-copy-target]");
      if (copyInput) {
        var target = document.querySelector(copyInput.getAttribute("data-copy-target"));
        if (target) {
          copyText(target.value);
          flashCopied(copyInput);
        }
      }
    });

    /* Destructive forms opt in with data-confirm="Are you sure?". */
    document.addEventListener("submit", function (event) {
      var message = event.target.getAttribute && event.target.getAttribute("data-confirm");
      if (message && !window.confirm(message)) {
        event.preventDefault();
      }
    });

    /*
     * SelectMenu filter. Primer ships the markup but no behaviour, and a filter box
     * that does not filter is worse than no filter box, so this wires it: type in a
     * .SelectMenu-input and the .SelectMenu-item siblings that do not match are
     * hidden. Case-insensitive substring match, the same as GitHub's.
     */
    document.addEventListener("input", function (event) {
      var input = event.target;
      if (!input.classList || !input.classList.contains("SelectMenu-input")) {
        return;
      }
      var modal = input.closest(".SelectMenu-modal");
      if (!modal) {
        return;
      }
      var needle = input.value.trim().toLowerCase();
      var items = modal.querySelectorAll(".SelectMenu-item");
      for (var i = 0; i < items.length; i++) {
        var text = (items[i].textContent || "").trim().toLowerCase();
        items[i].hidden = needle !== "" && text.indexOf(needle) === -1;
      }
    });

    /* Re-apply to markup HTMX swaps in. */
    document.body.addEventListener("htmx:afterSwap", function (event) {
      applyRelativeTimes(event.target);
      highlightAll(event.target);
    });
  });
})();
