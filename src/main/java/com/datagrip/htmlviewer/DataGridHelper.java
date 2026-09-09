package com.datagrip.htmlviewer;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataGridHelper {

    public static class CellData {
        private final String value;
        private final String columnName;

        public CellData(String value, String columnName) {
            this.value = value;
            this.columnName = columnName;
        }

        public String getValue() {
            return value;
        }

        public String getColumnName() {
            return columnName;
        }
    }

    public static boolean isRealColumnName(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return false;
        // Omit single-letter or spreadsheet column references (A, B, C, AA, etc.)
        if (trimmed.matches("^[A-Za-z]{1,3}$")) return false;
        // Omit generic index labels (Column 0, Column 1, Col 1, etc.)
        if (trimmed.matches("^(?i)(column|col)\\s*\\d+$")) return false;
        // Omit generic placeholder titles
        if (trimmed.equalsIgnoreCase("HTML Cell") ||
            trimmed.equalsIgnoreCase("Selected Item") ||
            trimmed.equalsIgnoreCase("Editor Selection") ||
            trimmed.equalsIgnoreCase("Editor Content") ||
            trimmed.equalsIgnoreCase("Database Cell")) {
            return false;
        }
        return true;
    }

    public static String extractRealColumnName(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String) {
            String s = ((String) obj).trim();
            return isRealColumnName(s) ? s : null;
        }
        String[] methods = {"getName", "getColumnName", "getDisplayName", "getTitle", "getFieldName", "getUnambiguousColumnName"};
        for (String mName : methods) {
            try {
                Method m = obj.getClass().getMethod(mName);
                Object res = m.invoke(obj);
                if (res != null) {
                    String s = res.toString().trim();
                    if (isRealColumnName(s)) return s;
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            Field f = obj.getClass().getDeclaredField("name");
            f.setAccessible(true);
            Object res = f.get(obj);
            if (res != null) {
                String s = res.toString().trim();
                if (isRealColumnName(s)) return s;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static CellData extractCellData(AnActionEvent e) {
        try {
            // Strategy 1: Check Swing JTable from context component / focus owner
            CellData tableData = extractFromSwingTable(e);
            if (tableData != null && isValidValue(tableData.getValue())) {
                return tableData;
            }

            // Strategy 2: Check DataGrip DataGrid via reflection
            CellData gridData = extractFromDataGrid(e);
            if (gridData != null && isValidValue(gridData.getValue())) {
                return gridData;
            }

            // Strategy 3: Check active Editor selection / document
            Editor editor = e.getData(CommonDataKeys.EDITOR);
            if (editor != null) {
                String selectedText = editor.getSelectionModel().getSelectedText();
                if (isValidValue(selectedText)) {
                    return new CellData(selectedText, null);
                }
                String docText = editor.getDocument().getText();
                if (isValidValue(docText)) {
                    return new CellData(docText, null);
                }
            }

            // Strategy 4: Try PlatformDataKeys.SELECTED_ITEMS or SELECTED_ITEM
            Object selectedItem = e.getData(PlatformDataKeys.SELECTED_ITEM);
            if (selectedItem != null) {
                String val = extractStringFromObject(selectedItem, new HashSet<>());
                if (isValidValue(val)) {
                    return new CellData(val, null);
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static boolean isValidValue(String val) {
        if (val == null) return false;
        String trimmed = val.trim();
        if (trimmed.isEmpty() || trimmed.equals("{}") || trimmed.equals("[]") || trimmed.equalsIgnoreCase("null")) {
            return false;
        }
        if (trimmed.equalsIgnoreCase("console") || trimmed.toLowerCase().endsWith(".sql") ||
            trimmed.startsWith("Query_") || trimmed.startsWith("Console_")) {
            return false;
        }
        return true;
    }

    private static CellData extractFromSwingTable(AnActionEvent e) {
        try {
            Component comp = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
            if (comp == null) {
                comp = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            }

            JTable table = findJTable(comp);
            if (table == null) return null;

            int row = table.getSelectedRow();
            int col = table.getSelectedColumn();

            if (row < 0 || col < 0 || row >= table.getRowCount() || col >= table.getColumnCount()) {
                return null;
            }

            String realColName = null;
            try {
                TableColumn tableColumn = table.getColumnModel().getColumn(col);
                if (tableColumn != null) {
                    realColName = extractRealColumnName(tableColumn.getHeaderValue());
                    if (realColName == null) {
                        realColName = extractRealColumnName(tableColumn.getIdentifier());
                    }
                }
            } catch (Throwable ignored) {
            }

            if (realColName == null) {
                try {
                    realColName = extractRealColumnName(table.getColumnName(col));
                } catch (Throwable ignored) {
                }
            }

            if (realColName == null) {
                try {
                    realColName = extractRealColumnName(table.getModel().getColumnName(col));
                } catch (Throwable ignored) {
                }
            }

            // Also attempt to get the real column name from DataGrid context if available
            if (realColName == null) {
                try {
                    CellData gridData = extractFromDataGrid(e);
                    if (gridData != null && isRealColumnName(gridData.getColumnName())) {
                        realColName = gridData.getColumnName();
                    }
                } catch (Throwable ignored) {
                }
            }

            Object val = null;
            try {
                val = table.getValueAt(row, col);
            } catch (Throwable ignored) {
            }

            if (val == null) {
                try {
                    val = table.getModel().getValueAt(row, col);
                } catch (Throwable ignored) {
                }
            }

            if (val != null) {
                String str = extractStringFromObject(val, new HashSet<>());
                if (isValidValue(str)) {
                    return new CellData(str, realColName);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static JTable findJTable(Component comp) {
        Component current = comp;
        while (current != null) {
            if (current instanceof JTable) {
                return (JTable) current;
            }
            if (current instanceof Container) {
                Container cont = (Container) current;
                for (Component child : cont.getComponents()) {
                    if (child instanceof JTable) {
                        return (JTable) child;
                    }
                }
            }
            current = current.getParent();
        }
        return null;
    }

    private static CellData extractFromDataGrid(AnActionEvent e) {
        try {
            String[] keyClasses = {
                "com.intellij.database.run.ui.DatabaseDataKeys",
                "com.intellij.database.datagrid.GridDataKeys",
                "com.intellij.database.datagrid.DatabaseDataKeysCore",
                "com.intellij.database.run.ui.grid.GridDataKeys"
            };

            Object gridInstance = null;

            for (String className : keyClasses) {
                try {
                    Class<?> clazz = Class.forName(className);
                    String[] fieldNames = {"DATA_GRID_KEY", "GRID_KEY", "DATA_GRID", "GRID_SELECTION_KEY"};
                    for (String fieldName : fieldNames) {
                        try {
                            Field field = clazz.getField(fieldName);
                            DataKey<?> key = (DataKey<?>) field.get(null);
                            Object data = e.getData(key);
                            if (data != null) {
                                gridInstance = data;
                                break;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                } catch (Throwable ignored) {
                }
                if (gridInstance != null) break;
            }

            if (gridInstance == null) return null;

            Method getSelectionModelMethod = gridInstance.getClass().getMethod("getSelectionModel");
            Object selectionModel = getSelectionModelMethod.invoke(gridInstance);
            if (selectionModel == null) return null;

            Method getSelectedRowsMethod = selectionModel.getClass().getMethod("getSelectedRows");
            Method getSelectedColumnsMethod = selectionModel.getClass().getMethod("getSelectedColumns");

            int[] selectedRows = (int[]) getSelectedRowsMethod.invoke(selectionModel);
            int[] selectedCols = (int[]) getSelectedColumnsMethod.invoke(selectionModel);

            if (selectedRows.length == 0 || selectedCols.length == 0) return null;

            int row = selectedRows[0];
            int col = selectedCols[0];

            Method getDataModelMethod = gridInstance.getClass().getMethod("getDataModel");
            Object dataModel = getDataModelMethod.invoke(gridInstance);
            if (dataModel == null) return null;

            String realColName = null;

            // Try GridModel.getColumns()
            try {
                Method getColumnsMethod = dataModel.getClass().getMethod("getColumns");
                Object colsObj = getColumnsMethod.invoke(dataModel);
                if (colsObj instanceof List) {
                    List<?> colsList = (List<?>) colsObj;
                    if (col >= 0 && col < colsList.size()) {
                        realColName = extractRealColumnName(colsList.get(col));
                    }
                }
            } catch (Throwable ignored) {
            }

            // Try getColumnName(col)
            if (realColName == null) {
                try {
                    Method getColumnNameMethod = dataModel.getClass().getMethod("getColumnName", int.class);
                    Object colNameObj = getColumnNameMethod.invoke(dataModel, col);
                    realColName = extractRealColumnName(colNameObj);
                } catch (Throwable ignored) {
                }
            }

            // Try getUnambiguousColumnName on gridInstance
            if (realColName == null) {
                for (Method m : gridInstance.getClass().getMethods()) {
                    if (m.getName().equals("getUnambiguousColumnName") && m.getParameterCount() == 1) {
                        try {
                            Object res = m.invoke(gridInstance, col);
                            realColName = extractRealColumnName(res);
                            if (realColName != null) break;
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }

            String[] gridMethods = {"getRawValue", "getDisplayValue", "getValue"};
            for (String methodName : gridMethods) {
                try {
                    Method m = gridInstance.getClass().getMethod(methodName, int.class, int.class);
                    Object val = m.invoke(gridInstance, row, col);
                    String extracted = extractStringFromObject(val, new HashSet<>());
                    if (isValidValue(extracted)) {
                        return new CellData(extracted, realColName);
                    }
                } catch (Throwable ignored) {
                }
            }

            String[] modelMethods = {"getValueAt", "getRawValueAt", "getUnformattedValue", "getString", "getText"};
            for (String methodName : modelMethods) {
                try {
                    Method m = dataModel.getClass().getMethod(methodName, int.class, int.class);
                    Object val = m.invoke(dataModel, row, col);
                    String extracted = extractStringFromObject(val, new HashSet<>());
                    if (isValidValue(extracted)) {
                        return new CellData(extracted, realColName);
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static String extractStringFromObject(Object obj, Set<Object> visited) {
        if (obj == null) return null;
        if (visited.contains(obj)) return null;
        visited.add(obj);

        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof byte[]) {
            return new String((byte[]) obj, StandardCharsets.UTF_8);
        }
        if (obj instanceof Object[]) {
            Object[] arr = (Object[]) obj;
            for (Object item : arr) {
                String str = extractStringFromObject(item, visited);
                if (isValidValue(str)) return str;
            }
        }
        if (obj instanceof Iterable) {
            for (Object item : (Iterable<?>) obj) {
                String str = extractStringFromObject(item, visited);
                if (isValidValue(str)) return str;
            }
        }

        String[] getterNames = {
            "getString", "getRawValue", "getDisplayValue", "getValue",
            "getUnformattedValue", "getText", "getRawText", "getContent",
            "asString", "getFormattedValue", "getParsedValue", "asPlainText",
            "getStr", "getData"
        };

        for (String getter : getterNames) {
            try {
                Method m = obj.getClass().getMethod(getter);
                Object res = m.invoke(obj);
                if (res != null && res != obj) {
                    String str = extractStringFromObject(res, visited);
                    if (isValidValue(str)) {
                        return str;
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        Class<?> current = obj.getClass();
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(obj);
                    if (val != null && val != obj) {
                        if (val instanceof String && isValidValue((String) val)) {
                            return (String) val;
                        }
                        if (val instanceof byte[]) {
                            String str = new String((byte[]) val, StandardCharsets.UTF_8);
                            if (isValidValue(str)) return str;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            current = current.getSuperclass();
        }

        String str = obj.toString();
        if (isValidValue(str)) {
            return str;
        }
        return null;
    }
}
