/* GitKoo client script — vanilla JS, no build step.
 *
 * Only handles things HTMX is a poor fit for (DESIGN.md §4):
 *   - theme toggle (persisted to localStorage)
 *   - clipboard copy for clone URLs
 *   - confirm dialogs for destructive actions
 *
 * Loaded once via the layout fragment; safe on every page.
 */
(function () {
  "use strict";

  var STORAGE_KEY = "gitkoo-theme";

  function applyTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    var toggle = document.querySelector(".theme-toggle");
    if (toggle) {
      toggle.textContent = theme === "dark" ? "\u2600\ufe0f" : "\u{1f313}";
    }
  }

  function initTheme() {
    var saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      applyTheme(saved);
    } else if (window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches) {
      applyTheme("dark");
    } else {
      applyTheme("light");
    }
  }

  function toggleTheme() {
    var current = document.documentElement.getAttribute("data-theme");
    var next = current === "dark" ? "light" : "dark";
    localStorage.setItem(STORAGE_KEY, next);
    applyTheme(next);
  }

  // Copy text to clipboard — used by clone box copy buttons.
  function copyText(text) {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text).catch(function () {});
    } else {
      var ta = document.createElement("textarea");
      ta.value = text;
      document.body.appendChild(ta);
      ta.select();
      try { document.execCommand("copy"); } catch (e) {}
      document.body.removeChild(ta);
    }
  }

  document.addEventListener("DOMContentLoaded", function () {
    initTheme();

    var toggle = document.querySelector(".theme-toggle");
    if (toggle) {
      toggle.addEventListener("click", toggleTheme);
    }

    // Clone box copy buttons: data-copy attribute holds the text to copy.
    document.querySelectorAll("[data-copy]").forEach(function (el) {
      el.addEventListener("click", function () {
        copyText(el.getAttribute("data-copy"));
        var original = el.textContent;
        el.textContent = "Copied!";
        setTimeout(function () { el.textContent = original; }, 1500);
      });
    });
  });
})();
