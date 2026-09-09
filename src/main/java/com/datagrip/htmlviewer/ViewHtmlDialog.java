package com.datagrip.htmlviewer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ViewHtmlDialog extends DialogWrapper {

    private final String rawHtml;
    private final String columnName;
    private HtmlViewerPanel viewerPanel;

    public ViewHtmlDialog(@Nullable Project project, String rawHtml, String columnName) {
        super(project, true);
        this.rawHtml = rawHtml;
        this.columnName = columnName;

        if (columnName != null && DataGridHelper.isRealColumnName(columnName)) {
            setTitle("Rendered HTML View - " + columnName);
        } else {
            setTitle("Rendered HTML View");
        }

        setModal(false); // Non-modal so user can keep it open while querying/working
        init();
        pack();
        setSize(900, 650);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        viewerPanel = new HtmlViewerPanel(rawHtml);
        return viewerPanel;
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }

    @Override
    protected void dispose() {
        if (viewerPanel != null) {
            viewerPanel.dispose();
        }
        super.dispose();
    }
}
