package com.datagrip.htmlviewer;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class ViewHtmlAction extends AnAction implements DumbAware {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        DataGridHelper.CellData cellData = DataGridHelper.extractCellData(e);
        if (cellData != null && cellData.getValue() != null && HtmlDetector.isHtml(cellData.getValue())) {
            e.getPresentation().setEnabledAndVisible(true);
        } else {
            e.getPresentation().setEnabledAndVisible(false);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        DataGridHelper.CellData cellData = DataGridHelper.extractCellData(e);

        if (cellData == null || cellData.getValue() == null || cellData.getValue().trim().isEmpty()) {
            Messages.showWarningDialog(project, "No cell value or selection found in current view.", "View Rendered HTML");
            return;
        }

        String rawHtml = cellData.getValue();
        String columnName = cellData.getColumnName();

        ViewHtmlDialog dialog = new ViewHtmlDialog(project, rawHtml, columnName);
        dialog.show();
    }
}