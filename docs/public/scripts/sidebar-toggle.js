const CHEVRON_LEFT = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"></polyline></svg>`;
const CHEVRON_RIGHT = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>`;

const SIDES = {
  left: {
    attr: "data-sidebar-collapsed",
    key: "pika-sidebar-collapsed",
    label: "Toggle navigation sidebar",
    expandedIcon: CHEVRON_LEFT,
    collapsedIcon: CHEVRON_RIGHT,
  },
  right: {
    attr: "data-toc-collapsed",
    key: "pika-toc-collapsed",
    label: "Toggle table of contents",
    expandedIcon: CHEVRON_RIGHT,
    collapsedIcon: CHEVRON_LEFT,
  },
};

function isCollapsed(side) {
  return document.documentElement.hasAttribute(SIDES[side].attr);
}

function updateIcon(btn, side) {
  const config = SIDES[side];
  btn.innerHTML = isCollapsed(side) ? config.collapsedIcon : config.expandedIcon;
}

function toggle(side) {
  const { attr, key } = SIDES[side];
  const html = document.documentElement;

  if (html.hasAttribute(attr)) {
    html.removeAttribute(attr);
    localStorage.setItem(key, "false");
  } else {
    html.setAttribute(attr, "");
    localStorage.setItem(key, "true");
  }

  document.querySelectorAll(".sidebar-toggle-btn").forEach((btn) => {
    const btnSide = btn.classList.contains("sidebar-toggle-left")
      ? "left"
      : "right";
    updateIcon(btn, btnSide);
  });
}

function createToggleButton(side) {
  const config = SIDES[side];
  const btn = document.createElement("button");
  btn.className = `sidebar-toggle-btn sidebar-toggle-${side}`;
  btn.setAttribute("aria-label", config.label);
  btn.setAttribute("title", config.label);
  updateIcon(btn, side);
  btn.addEventListener("click", () => toggle(side));
  document.body.appendChild(btn);
}

if (document.documentElement.hasAttribute("data-has-sidebar")) {
  createToggleButton("left");
}
if (document.documentElement.hasAttribute("data-has-toc")) {
  createToggleButton("right");
}
