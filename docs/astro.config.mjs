import { defineConfig } from "astro/config";
import starlight from "@astrojs/starlight";
import { visit } from "unist-util-visit";

function remarkMermaid() {
  return (tree) => {
    visit(tree, "code", (node, index, parent) => {
      if (node.lang === "mermaid") {
        parent.children[index] = {
          type: "html",
          value: `<pre class="mermaid">${node.value}</pre>`,
        };
      }
    });
  };
}

export default defineConfig({
  site: "https://lukmccall.github.io",
  base: "/pika",
  integrations: [
    starlight({
      title: "Pika",
      logo: {
        src: "./src/assets/logo.png",
      },
      social: [
        {
          icon: "github",
          label: "GitHub",
          href: "https://github.com/lukmccall/pika",
        },
      ],
      customCss: ["./src/styles/custom.css"],
      head: [
        {
          tag: "script",
          content: `(function(){
            if(localStorage.getItem('pika-sidebar-collapsed')==='true')
              document.documentElement.setAttribute('data-sidebar-collapsed','');
            if(localStorage.getItem('pika-toc-collapsed')==='true')
              document.documentElement.setAttribute('data-toc-collapsed','');
          })();`,
        },
        {
          tag: "script",
          attrs: {
            type: "module",
            src: "/pika/scripts/sidebar-toggle.js",
          },
        },
        {
          tag: "script",
          attrs: {
            type: "module",
          },
          content: `
            import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs';
            const isDark = document.documentElement.dataset.theme === 'dark';
            mermaid.initialize({
              startOnLoad: true,
              theme: isDark ? 'dark' : 'default',
            });

            const overlay = document.createElement('div');
            overlay.id = 'mermaid-overlay';
            document.body.appendChild(overlay);
            overlay.addEventListener('click', () => overlay.classList.remove('active'));
            document.addEventListener('keydown', (e) => {
              if (e.key === 'Escape') overlay.classList.remove('active');
            });

            const observer = new MutationObserver(() => {
              document.querySelectorAll('pre.mermaid[data-processed]').forEach((pre) => {
                if (pre.dataset.clickBound) return;
                pre.dataset.clickBound = 'true';
                pre.style.cursor = 'pointer';
                pre.setAttribute('title', 'Click to enlarge');
                pre.addEventListener('click', () => {
                  overlay.innerHTML = pre.innerHTML;
                  overlay.classList.add('active');
                });
              });
            });
            observer.observe(document.body, { childList: true, subtree: true, attributes: true });
          `,
        },
      ],
      sidebar: [
        { label: "Home", slug: "index" },
        { label: "Getting Started", slug: "getting-started" },
        {
          label: "Internals",
          items: [
            {
              label: "typeDescriptorOf: Deep Dive",
              slug: "internals/type-descriptor-of",
            },
            {
              label: "@Introspectable: Deep Dive",
              slug: "internals/introspectable",
            },
          ],
        },
      ],
    }),
  ],
  markdown: {
    remarkPlugins: [remarkMermaid],
  },
});
