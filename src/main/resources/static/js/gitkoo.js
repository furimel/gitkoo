/* GitKoo client script - vanilla JS, no build step.
 *
 * Only handles what HTMX is a poor fit for (DESIGN.md 4):
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

  /* ── Wiring ────────────────────────────────────────────────────────── */

  document.addEventListener("DOMContentLoaded", function () {
    syncToggleIcons();
    applyRelativeTimes(document);

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

    /* Re-apply to markup HTMX swaps in. */
    document.body.addEventListener("htmx:afterSwap", function (event) {
      applyRelativeTimes(event.target);
    });
  });
})();
