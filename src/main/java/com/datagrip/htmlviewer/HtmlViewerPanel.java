package com.datagrip.htmlviewer;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class HtmlViewerPanel extends JPanel {

    private final String rawHtmlContent;
    private String processedHtml;

    private JBCefBrowser jcefBrowser;
    private JEditorPane swingEditorPane;
    private JTextArea sourceCodeArea;
    private JBTabbedPane tabbedPane;

    public HtmlViewerPanel(String rawHtmlContent) {
        this.rawHtmlContent = rawHtmlContent != null ? rawHtmlContent : "";
        this.processedHtml = HtmlDetector.prepareHtmlForRendering(this.rawHtmlContent);

        setLayout(new BorderLayout());
        setBackground(UIUtil.getPanelBackground());
        initUI();
    }

    private void initUI() {
        // Toolbar on top using JPanel with FlowLayout matching IDE theme (dark/light)
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolbar.setBackground(UIUtil.getPanelBackground());
        toolbar.setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0));

        JButton btnCopy = new JButton("Copy HTML");
        btnCopy.setToolTipText("Copy raw HTML content to clipboard");
        btnCopy.addActionListener(e -> {
            CopyPasteManager.getInstance().setContents(new StringSelection(rawHtmlContent));
            Messages.showInfoMessage(this, "HTML content copied to clipboard!", "Copied");
        });

        JButton btnOpenBrowser = new JButton("Open in External Browser");
        btnOpenBrowser.setToolTipText("Save temporary HTML file and open in default system web browser");
        btnOpenBrowser.addActionListener(e -> openInExternalBrowser());

        toolbar.add(btnCopy);
        toolbar.add(btnOpenBrowser);

        add(toolbar, BorderLayout.NORTH);

        // Tabbed view: Rendered View & Source Code
        tabbedPane = new JBTabbedPane();

        // 1. Rendered View Panel
        JComponent renderComponent = createRenderComponent();
        tabbedPane.addTab("Rendered View", renderComponent);

        // 2. Source Code View Panel
        sourceCodeArea = new JTextArea(rawHtmlContent);
        sourceCodeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        sourceCodeArea.setEditable(false);
        sourceCodeArea.setLineWrap(true);
        sourceCodeArea.setWrapStyleWord(true);
        sourceCodeArea.setCaretPosition(0);
        sourceCodeArea.setBackground(UIUtil.getTextFieldBackground());
        sourceCodeArea.setForeground(UIUtil.getTextFieldForeground());
        sourceCodeArea.setCaretColor(UIUtil.getTextFieldForeground());

        JBScrollPane sourceScrollPane = new JBScrollPane(sourceCodeArea);
        sourceScrollPane.setBorder(JBUI.Borders.empty());
        tabbedPane.addTab("HTML Source", sourceScrollPane);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JComponent createRenderComponent() {
        if (JBCefApp.isSupported()) {
            try {
                jcefBrowser = new JBCefBrowser();

                // Intercept navigation to prevent links from opening while keeping hover hand icon
                jcefBrowser.getJBCefClient().addRequestHandler(new CefRequestHandlerAdapter() {
                    @Override
                    public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean user_gesture, boolean is_redirect) {
                        if (user_gesture) {
                            return true; // Cancel navigation on user clicking a link
                        }
                        return false;
                    }

                    @Override
                    public boolean onOpenURLFromTab(CefBrowser browser, CefFrame frame, String target_url, boolean user_gesture) {
                        return true; // Cancel opening URL
                    }
                }, jcefBrowser.getCefBrowser());

                // Prevent links with target="_blank" from opening popups or new windows
                jcefBrowser.getJBCefClient().addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
                    @Override
                    public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String target_url, String target_frame_name) {
                        return true; // Cancel popup
                    }
                }, jcefBrowser.getCefBrowser());

                jcefBrowser.loadHTML(processedHtml);
                return jcefBrowser.getComponent();
            } catch (Exception ex) {
                // Fallback to Swing if JCEF initialization fails
            }
        }

        // Swing JEditorPane fallback
        swingEditorPane = new JEditorPane();
        swingEditorPane.setEditable(false);
        swingEditorPane.setContentType("text/html");

        // Show hand cursor when hovering over links, without navigating when clicked
        swingEditorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                swingEditorPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
                swingEditorPane.setCursor(Cursor.getDefaultCursor());
            }
            // Intentionally omit EventType.ACTIVATED so link clicks do not navigate
        });

        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("body { font-family: sans-serif; padding: 10px; }");
        styleSheet.addRule("a, a * { cursor: pointer; }");
        swingEditorPane.setEditorKit(kit);
        swingEditorPane.setText(processedHtml);
        swingEditorPane.setCaretPosition(0);

        JBScrollPane scrollPane = new JBScrollPane(swingEditorPane);
        scrollPane.setBorder(JBUI.Borders.empty());
        return scrollPane;
    }

    private void openInExternalBrowser() {
        try {
            java.io.File tempFile = java.io.File.createTempFile("datagrip_html_preview_", ".html");
            tempFile.deleteOnExit();
            java.nio.file.Files.writeString(tempFile.toPath(), processedHtml);
            BrowserUtil.browse(tempFile.toURI().toString());
        } catch (Exception ex) {
            Messages.showErrorDialog(this, "Failed to open HTML in browser: " + ex.getMessage(), "Error");
        }
    }

    public void dispose() {
        if (jcefBrowser != null) {
            jcefBrowser.dispose();
        }
    }
}
