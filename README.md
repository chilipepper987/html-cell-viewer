# HTML Cell Viewer

A JetBrains plugin for DataGrip and IntelliJ Database Tools that lets you render HTML content contained in database column cells into a live, interactive web browser view.

---

## 🌟 Key Features

1. **Cell Context Menu Preview ("View Rendered HTML")**:
   - Right-click any database cell containing HTML in query results or table editors and select **View Rendered HTML** (or press `Ctrl+Alt+H` on Windows/Linux or `Option+Shift+H` on macOS).
2. **High-Fidelity Chromium (JCEF) Browser**:
   - Renders HTML5, CSS3, SVG, inline/remote images, tables, and custom styling using JetBrains' built-in `JBCefBrowser` with Swing `JEditorPane` fallback.
3. **Strict Content Security Policy (CSP)**:
   - Blocks arbitrary JavaScript execution and inline scripts to protect against XSS and local network probing while preserving full HTML/CSS rendering.
4. **Smart HTML Detection**:
   - Automatically detects whether a selected cell contains HTML, unescapes double-encoded entities (like `&lt;div&gt;`), and conditionally shows the context menu action only for HTML-valued fields.
5. **Non-Navigable Links with Hover Hand Cursor**:
   - Links in the preview display the standard pointer / hand icon when hovered, but clicks do not navigate or leave the preview window.
6. **Dual Tabbed Preview**:
   - **Rendered View**: Full interactive browser view.
   - **HTML Source**: Formatted view of the raw HTML markup.
7. **Toolbox Controls**:
   - **Copy HTML**: One-click copy of raw HTML to clipboard.
   - **Open in External Browser**: Opens the cell's HTML content directly in your system default browser.

---

## 🚀 Building & Packaging

### Requirements
- **JDK**: Java 17 or higher
- **Gradle**: 8.x (or Gradle Wrapper)

### Build Instructions

```bash
cd datagrip-html-viewer
./gradlew buildPlugin
```
*The compiled plugin file will be generated at:*  
`build/distributions/html-cell-viewer-1.0.0.zip`

---

## 💡 How to Use

1. Open a database table or execute a SQL query returning HTML data.
2. Select or right-click any cell containing HTML in the result grid.
3. Click **View Rendered HTML** (or press `Ctrl+Alt+H` / `Option+Shift+H`).
4. An interactive window will open displaying the rendered HTML.

---

## 📄 License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.
