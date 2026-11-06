import { useState } from "react";
import { Sun, Moon } from "lucide-react";
const KEY = "gapmap-theme";
function apply(theme) {
  document.documentElement.dataset.theme = theme;
  try {
    localStorage.setItem(KEY, theme);
  } catch {}
  document
    .querySelector('meta[name="theme-color"]')
    ?.setAttribute("content", theme === "dark" ? "#2a2f3a" : "#e0e5ec");
}
export default function ThemeToggle({ floating = false }) {
  const [dark, setDark] = useState(
    () => document.documentElement.dataset.theme === "dark",
  );
  function toggle() {
    apply(dark ? "light" : "dark");
    setDark(!dark);
  }
  return (
    <button
      type="button"
      role="switch"
      aria-checked={dark}
      aria-label="Dark mode"
      title={dark ? "Switch to light mode" : "Switch to dark mode"}
      className={"theme-toggle" + (floating ? " floating" : "")}
      onClick={toggle}
    >
      <span className="tt-track">
        <span className="tt-knob">
          {dark ? <Moon size={14} /> : <Sun size={14} />}
        </span>
      </span>
    </button>
  );
}
