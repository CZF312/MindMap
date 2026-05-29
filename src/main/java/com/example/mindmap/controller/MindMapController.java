package com.example.mindmap.controller;

import com.example.mindmap.command.AddNodeCommand;
import com.example.mindmap.command.ChangeLayoutCommand;
import com.example.mindmap.command.ChangeStyleCommand;
import com.example.mindmap.command.Command;
import com.example.mindmap.command.DeleteNodeCommand;
import com.example.mindmap.command.MoveNodeCommand;
import com.example.mindmap.command.RenameNodeCommand;
import com.example.mindmap.command.ToggleCollapseCommand;
import com.example.mindmap.model.ConnectionShape;
import com.example.mindmap.model.ConnectionStyle;
import com.example.mindmap.model.LayoutType;
import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;
import com.example.mindmap.model.SelectionModel;
import com.example.mindmap.service.ImageExportService;
import com.example.mindmap.service.MindMapFileService;
import com.example.mindmap.service.MindMapLayoutService;
import com.example.mindmap.service.RecentFileService;
import com.example.mindmap.service.SearchService;
import com.example.mindmap.service.EditHistory;
import com.example.mindmap.service.SearchState;
import com.example.mindmap.util.Dialogs;
import com.example.mindmap.util.GeometryUtils;
import com.example.mindmap.view.MainFrame;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.FileNotFoundException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class MindMapController {
    private final Stage stage;
    private final MainFrame mainFrame;
    private final MindMapLayoutService layoutService = new MindMapLayoutService();
    private final MindMapFileService fileService = new MindMapFileService();
    private final ImageExportService imageExportService = new ImageExportService();
    private final SearchService searchService = new SearchService();
    private final RecentFileService recentFileService = new RecentFileService();
    private final SelectionModel selectionModel = new SelectionModel();
    private final EditHistory editHistory = new EditHistory();
    private final SearchState searchState = new SearchState();
    private MindMap currentMap = new MindMap();
    private MindMap dragBefore;

    private enum ExportFormat {
        PNG, JPG, PDF
    }

    public MindMapController(Stage stage) {
        this.stage = stage;
        this.mainFrame = new MainFrame(this);
        stage.setOnCloseRequest(event -> {
            if (!confirmBeforeDestructiveAction()) {
                event.consume();
            }
        });
    }

    public MainFrame getMainFrame() {
        return mainFrame;
    }

    public void newMapSilently() {
        currentMap = new MindMap();
        selectionModel.selectOnly(currentMap.getRoot());
        editHistory.clear();
        searchState.setResults(List.of());
        refreshAll("已新建思维导图", true);
    }

    public void newMap() {
        if (!confirmBeforeDestructiveAction()) {
            return;
        }
        newMapSilently();
    }

    public void openMap() {
        if (!confirmBeforeDestructiveAction()) {
            return;
        }
        FileChooser chooser = mindMapChooser("打开思维导图");
        Path path = showOpen(chooser);
        if (path != null) {
            openPath(path, false);
        }
    }

    public void openRecentFile(Path path) {
        if (!confirmBeforeDestructiveAction()) {
            return;
        }
        if (!recentFileService.exists(path)) {
            recentFileService.remove(path);
            refreshRecentFiles();
            Dialogs.error(stage, "打开失败", "最近文件不存在，已从列表移除。");
            return;
        }
        openPath(path, true);
    }

    private void openPath(Path path, boolean recent) {
        try {
            MindMap loaded = fileService.load(path);
            currentMap = loaded;
            selectionModel.selectOnly(currentMap.getRoot());
            editHistory.clear();
            searchState.setResults(List.of());
            recentFileService.add(path);
            refreshAll((recent ? "已打开最近文件：" : "打开成功：") + path, false);
        } catch (Exception ex) {
            Dialogs.error(stage, "打开失败", ex.getMessage());
            refreshAll("打开失败，当前导图未改变", false);
        }
    }

    public boolean saveMap() {
        if (currentMap.getFilePath() == null) {
            return saveMapAs();
        }
        try {
            return saveTo(currentMap.getFilePath());
        } catch (Exception ex) {
            if (shouldOfferSaveAs(ex)
                    && Dialogs.confirm(stage, "保存失败",
                    "原文件所在位置不可写或文件已不可用，是否另存为到其他位置？")) {
                return saveMapAs();
            }
            Dialogs.error(stage, "保存失败", ex.getMessage());
            mainFrame.setStatus("保存失败");
            return false;
        }
    }

    public boolean saveMapAs() {
        FileChooser chooser = mindMapChooser("保存思维导图");
        Path path = showSave(chooser);
        if (path == null) {
            mainFrame.setStatus("已取消保存");
            return false;
        }
        try {
            return saveTo(ensureExtension(path, ".mindmap"));
        } catch (Exception ex) {
            Dialogs.error(stage, "保存失败", ex.getMessage());
            mainFrame.setStatus("保存失败");
            return false;
        }
    }

    private boolean saveTo(Path path) throws Exception {
        fileService.save(currentMap, path);
        recentFileService.add(path);
        refreshAll("保存成功：" + path, false);
        return true;
    }

    private boolean shouldOfferSaveAs(Exception ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof AccessDeniedException || current instanceof FileNotFoundException) {
                return true;
            }
            current = current.getCause();
        }
        String message = ex.getMessage();
        return message != null && (message.contains("拒绝访问") || message.contains("Access is denied"));
    }

    public void exportPng() {
        exportImage("导出 PNG 图片", ".png", ExportFormat.PNG);
    }

    public void exportJpg() {
        exportImage("导出 JPG 图片", ".jpg", ExportFormat.JPG);
    }

    public void exportPdf() {
        exportImage("导出 PDF 文档", ".pdf", ExportFormat.PDF);
    }

    private void exportImage(String title, String extension, ExportFormat format) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extension.toUpperCase() + " 文件", "*" + extension));
        Path path = showSave(chooser);
        if (path == null) {
            mainFrame.setStatus("已取消导出");
            return;
        }
        Path exportPath = ensureExtension(path, extension);
        Label messageLabel = new Label("正在生成高清画布快照...");
        ProgressBar progressBar = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        Dialog<Void> progressDialog = exportProgressDialog(messageLabel, progressBar);
        progressDialog.show();
        mainFrame.setStatus("正在导出：" + exportPath);
        Platform.runLater(() -> startExportTask(format, exportPath, progressDialog, messageLabel, progressBar));
    }

    private Dialog<Void> exportProgressDialog(Label message, ProgressBar progress) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.setTitle("正在导出");
        dialog.setHeaderText(null);
        message.getStyleClass().add("export-progress-message");
        progress.getStyleClass().add("export-progress-bar");
        progress.setPrefWidth(340);
        VBox content = new VBox(12, message, progress);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        ButtonType hideButton = new ButtonType("后台运行", ButtonType.CANCEL.getButtonData());
        dialog.getDialogPane().getButtonTypes().setAll(hideButton);
        return dialog;
    }

    private void startExportTask(ExportFormat format, Path path, Dialog<Void> dialog,
                                 Label messageLabel, ProgressBar progressBar) {
        WritableImage snapshot;
        try {
            snapshot = imageExportService.createExportSnapshot(mainFrame.getCanvas());
        } catch (Exception ex) {
            dialog.hide();
            Dialogs.error(stage, "导出失败", ex.getMessage());
            mainFrame.setStatus("导出失败");
            return;
        }
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("正在写入 " + formatLabel(format) + " 文件...");
                updateProgress(ProgressBar.INDETERMINATE_PROGRESS, 1);
            switch (format) {
                    case PNG -> imageExportService.exportPng(snapshot, path);
                    case JPG -> imageExportService.exportJpg(snapshot, path);
                    case PDF -> imageExportService.exportPdf(snapshot, path);
                }
                updateMessage("导出完成");
                updateProgress(1, 1);
                return null;
            }
        };
        messageLabel.textProperty().bind(task.messageProperty());
        progressBar.progressProperty().bind(task.progressProperty());
        task.setOnSucceeded(event -> {
            dialog.hide();
            mainFrame.setStatus("导出成功：" + path);
        });
        task.setOnFailed(event -> {
            dialog.hide();
            Throwable ex = task.getException();
            Dialogs.error(stage, "导出失败", ex == null ? "未知错误" : ex.getMessage());
            mainFrame.setStatus("导出失败");
        });
        Thread thread = new Thread(task, "mindmap-export");
        thread.setDaemon(true);
        thread.start();
    }

    private String formatLabel(ExportFormat format) {
        return switch (format) {
            case PNG -> "PNG";
            case JPG -> "JPG";
            case PDF -> "PDF";
        };
    }

    public void addChildNode() {
        MindNode parent = primaryNode();
        if (parent == null) {
            notice("请先选择一个节点");
            return;
        }
        Dialogs.input(stage, "添加子节点", "节点文本：", "新节点")
                .ifPresent(text -> executeAndRefresh(new AddNodeCommand(currentMap, () -> {
                    MindNode liveParent = find(parent.getId()).orElse(currentMap.getRoot());
                    MindNode child = currentMap.createNode(text);
                    liveParent.addChild(child);
                    selectionModel.selectOnly(child);
                }), "已添加子节点", true));
    }

    public void addSiblingNode() {
        MindNode node = primaryNode();
        if (node == null) {
            notice("请先选择一个节点");
            return;
        }
        if (node.isRoot()) {
            notice("中心节点不能添加兄弟节点");
            return;
        }
        Dialogs.input(stage, "添加兄弟节点", "节点文本：", "新节点")
                .ifPresent(text -> executeAndRefresh(new AddNodeCommand(currentMap, () -> {
                    MindNode liveNode = find(node.getId()).orElseThrow();
                    MindNode parent = liveNode.getParent();
                    MindNode sibling = currentMap.createNode(text);
                    parent.addChild(parent.getChildren().indexOf(liveNode) + 1, sibling);
                    selectionModel.selectOnly(sibling);
                }), "已添加兄弟节点", true));
    }

    public void deleteSelectedNodes() {
        List<MindNode> deletable = selectedNodes().stream().filter(node -> !node.isRoot()).toList();
        if (deletable.isEmpty()) {
            notice(selectionModel.getSelectedNodeIds().isEmpty() ? "请先选择一个节点" : "中心节点不能删除");
            return;
        }
        if (!Dialogs.confirm(stage, "确认删除", "删除选中节点会同时删除其所有子节点，是否继续？")) {
            return;
        }
        List<String> topLevelIds = topLevelSelected(deletable).stream().map(MindNode::getId).toList();
        String fallbackParent = deletable.get(0).getParent() == null ? currentMap.getRoot().getId() : deletable.get(0).getParent().getId();
        executeAndRefresh(new DeleteNodeCommand(currentMap, () -> {
            for (String id : topLevelIds) {
                find(id).ifPresent(node -> {
                    MindNode parent = node.getParent();
                    if (parent != null) {
                        parent.removeChild(node);
                    }
                });
            }
            find(fallbackParent).ifPresentOrElse(selectionModel::selectOnly, () -> selectionModel.selectOnly(currentMap.getRoot()));
        }), "已删除选中节点", true);
    }

    public void renameSelectedNode() {
        MindNode node = primaryNode();
        if (node == null) {
            notice("请先选择一个节点");
            return;
        }
        renameNode(node);
    }

    public void renameNode(MindNode node) {
        Dialogs.input(stage, "重命名节点", "节点文本：", node.getText())
                .filter(text -> !text.isBlank())
                .ifPresent(text -> renameNodeText(node.getId(), text));
    }

    public boolean renameNodeText(String nodeId, String text) {
        if (text == null || text.isBlank()) {
            refreshAll("节点文本不能为空", false);
            return false;
        }
        executeAndRefresh(new RenameNodeCommand(currentMap, () -> {
            find(nodeId).ifPresent(live -> live.setText(text));
            selectionModel.setPrimaryNodeId(nodeId);
        }), "已重命名节点", true);
        return true;
    }

    public void changeFillColor(Color color) {
        changeSelectedStyle(color, true);
    }

    public void changeBorderColor(Color color) {
        changeSelectedStyle(color, false);
    }

    public void changeTextColor(Color color) {
        List<String> ids = new ArrayList<>(selectionModel.getSelectedNodeIds());
        if (ids.isEmpty()) {
            notice("请先选择一个或多个节点");
            return;
        }
        executeAndRefresh(new ChangeStyleCommand(currentMap, () -> {
            for (String id : ids) {
                find(id).ifPresent(node -> node.getStyle().setTextColor(color));
            }
        }), "已设置文字颜色", false);
    }

    public void changeFontFamily(String family) {
        updateSelectedTextStyle(style -> style.setFontFamily(family), "已设置字体", true);
    }

    public void changeFontSize(double size) {
        updateSelectedTextStyle(style -> style.setFontSize(size), "已设置字号", true);
    }

    public void toggleBold() {
        updateSelectedTextStyle(style -> style.setBold(!style.isBold()), "已切换加粗");
    }

    public void toggleItalic() {
        updateSelectedTextStyle(style -> style.setItalic(!style.isItalic()), "已切换斜体");
    }

    public void toggleUnderline() {
        updateSelectedTextStyle(style -> style.setUnderline(!style.isUnderline()), "已切换下划线");
    }

    public void toggleStrikethrough() {
        updateSelectedTextStyle(style -> style.setStrikethrough(!style.isStrikethrough()), "已切换删除线");
    }

    public void changeTextAlignment(TextAlignment alignment) {
        updateSelectedTextStyle(style -> style.setAlignment(alignment), "已设置文本对齐");
    }

    public void applyNodePreset(Color fillColor, Color textColor) {
        List<String> ids = new ArrayList<>(selectionModel.getSelectedNodeIds());
        if (ids.isEmpty()) {
            notice("请先选择一个或多个节点");
            return;
        }
        executeAndRefresh(new ChangeStyleCommand(currentMap, () -> {
            for (String id : ids) {
                find(id).ifPresent(node -> {
                    node.getStyle().setFillColor(fillColor);
                    node.getStyle().setTextColor(textColor);
                });
            }
        }), "已应用节点样式", false);
    }

    public void changeCanvasColor(Color color) {
        executeAndRefresh(new ChangeStyleCommand(currentMap, () -> currentMap.setCanvasColor(color)),
                "已设置画布背景色", false);
    }

    public void chooseCanvasBackgroundImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择画布背景图片");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );
        Path path = showOpen(chooser);
        if (path == null) {
            return;
        }
        String uri = path.toUri().toString();
        executeAndRefresh(new ChangeStyleCommand(currentMap, () -> currentMap.setCanvasImageUri(uri)),
                "已设置画布背景图片", false);
    }

    public void clearCanvasBackgroundImage() {
        if (currentMap.getCanvasImageUri() == null) {
            notice("当前没有画布背景图片");
            return;
        }
        executeAndRefresh(new ChangeStyleCommand(currentMap, () -> currentMap.setCanvasImageUri(null)),
                "已清除画布背景图片", false);
    }

    public void changeConnectionColor(Color color) {
        updateConnectionStyle(style -> style.setColor(color),
                () -> currentMap.setConnectionColor(color), "已设置连接线颜色");
    }

    public void changeConnectionWidth(double width) {
        updateConnectionStyle(style -> style.setWidth(width),
                () -> currentMap.setConnectionWidth(width), "已设置连接线粗细");
    }

    public void toggleConnectionDashed() {
        changeConnectionDashed(!activeConnectionStyle().isDashed());
    }

    public void changeConnectionDashed(boolean dashed) {
        updateConnectionStyle(style -> style.setDashed(dashed),
                () -> currentMap.setConnectionDashed(dashed),
                dashed ? "已切换为虚线" : "已切换为实线");
    }

    public void changeConnectionShape(ConnectionShape shape) {
        updateConnectionStyle(style -> style.setShape(shape),
                () -> currentMap.setConnectionShape(shape),
                "已设置连接线样式：" + shape.getLabel());
    }

    private void updateConnectionStyle(java.util.function.Consumer<ConnectionStyle> selectedChange,
                                       Runnable defaultChange,
                                       String status) {
        List<String> ids = new ArrayList<>(selectionModel.getSelectedConnectionIds());
        executeAndRefresh(new ChangeStyleCommand(currentMap, () -> {
            if (ids.isEmpty()) {
                defaultChange.run();
            } else {
                for (String id : ids) {
                    find(id).ifPresent(node -> selectedChange.accept(node.getConnectionStyle()));
                }
            }
        }), status, false);
    }

    private ConnectionStyle activeConnectionStyle() {
        String id = selectionModel.getPrimaryConnectionId();
        if (id != null) {
            Optional<MindNode> node = find(id);
            if (node.isPresent()) {
                return node.get().getConnectionStyle();
            }
        }
        return currentMap.defaultConnectionStyle();
    }

    private void changeSelectedStyle(Color color, boolean fill) {
        List<String> ids = new ArrayList<>(selectionModel.getSelectedNodeIds());
        if (ids.isEmpty()) {
            notice("请先选择一个或多个节点");
            return;
        }
        executeAndRefresh(new ChangeStyleCommand(currentMap, () -> {
            for (String id : ids) {
                find(id).ifPresent(node -> {
                    if (fill) {
                        node.getStyle().setFillColor(color);
                    } else {
                        node.getStyle().setBorderColor(color);
                    }
                });
            }
        }), fill ? "已设置填充色" : "已设置边框色", false);
    }

    private void updateSelectedTextStyle(java.util.function.Consumer<com.example.mindmap.model.NodeStyle> change, String status) {
        updateSelectedTextStyle(change, status, false);
    }

    private void updateSelectedTextStyle(java.util.function.Consumer<com.example.mindmap.model.NodeStyle> change,
                                         String status, boolean relayout) {
        List<String> ids = new ArrayList<>(selectionModel.getSelectedNodeIds());
        if (ids.isEmpty()) {
            notice("请先选择一个或多个节点");
            return;
        }
        executeAndRefresh(new ChangeStyleCommand(currentMap, () -> {
            for (String id : ids) {
                find(id).ifPresent(node -> change.accept(node.getStyle()));
            }
        }), status, relayout);
    }

    public void toggleCollapse() {
        MindNode node = primaryNode();
        if (node == null) {
            notice("请先选择一个节点");
            return;
        }
        toggleCollapse(node);
    }

    public void toggleCollapse(MindNode node) {
        if (node.getChildren().isEmpty()) {
            notice("该节点没有可折叠的子节点");
            return;
        }
        executeAndRefresh(new ToggleCollapseCommand(currentMap, () -> {
            find(node.getId()).ifPresent(live -> live.setCollapsed(!live.isCollapsed()));
            selectionModel.setPrimaryNodeId(node.getId());
        }), node.isCollapsed() ? "已展开节点" : "已折叠节点", true);
    }

    public void expandAll() {
        executeAndRefresh(new ToggleCollapseCommand(currentMap, () ->
                currentMap.allNodes().forEach(node -> node.setCollapsed(false))),
                "已展开全部节点", true);
    }

    public void collapseAll() {
        executeAndRefresh(new ToggleCollapseCommand(currentMap, () -> currentMap.allNodes().forEach(node -> {
            if (!node.isRoot() && !node.getChildren().isEmpty()) {
                node.setCollapsed(true);
            } else if (node.isRoot()) {
                node.setCollapsed(false);
            }
        })), "已收起全部分支", true);
    }

    public void changeLayout(LayoutType type) {
        if (type == currentMap.getLayoutType()) {
            refreshAll("当前已是" + type.getLabel(), true);
            return;
        }
        executeAndRefresh(new ChangeLayoutCommand(currentMap, () -> {
            currentMap.setLayoutType(type);
            currentMap.allNodes().forEach(node -> {
                node.setOffsetX(0);
                node.setOffsetY(0);
            });
        }), "已切换为" + type.getLabel(), true);
    }

    public void undo() {
        if (!editHistory.undo()) {
            notice("没有可撤销操作");
            return;
        }
        refreshAll("已撤销", true);
    }

    public void redo() {
        if (!editHistory.redo()) {
            notice("没有可重做操作");
            return;
        }
        refreshAll("已重做", true);
    }

    public void search(String keyword) {
        searchState.setResults(searchService.search(currentMap, keyword).stream().map(MindNode::getId).toList(), keyword);
        refreshAll(searchState.isEmpty()
                ? (keyword == null || keyword.isBlank() ? "已清空搜索" : "未找到匹配节点")
                : "搜索到 " + searchState.size() + " 个匹配节点", false);
    }

    public void nextSearchResult() {
        navigateSearch(1);
    }

    public void previousSearchResult() {
        navigateSearch(-1);
    }

    public void showFindReplaceDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.setTitle("查找和替换");
        TextField findField = new TextField();
        findField.textProperty().addListener((obs, oldValue, newValue) -> search(newValue));
        findField.setPromptText("查找内容");
        TextField replaceFindField = new TextField();
        replaceFindField.setPromptText("查找内容");
        replaceFindField.textProperty().addListener((obs, oldValue, newValue) -> search(newValue));
        TextField replaceField = new TextField();
        replaceField.setPromptText("替换为");
        Label findMatchLabel = new Label("找到 0 个匹配项");
        Label replaceMatchLabel = new Label("找到 0 个匹配项");
        findField.setPrefWidth(320);
        replaceFindField.setPrefWidth(320);
        replaceField.setPrefWidth(320);

        Button findPrevious = new Button("上一处(B)");
        Button findNext = new Button("下一处(F)");
        Button replacePrevious = new Button("替换上一处(B)");
        Button replaceCurrent = new Button("替换当前(R)");
        Button replaceNext = new Button("替换下一处(F)");
        Button replaceAll = new Button("全部替换(A)");

        Runnable refreshMatchLabel = () -> {
            String text = "找到 " + searchState.size() + " 个匹配项";
            findMatchLabel.setText(text);
            replaceMatchLabel.setText(text);
        };
        findField.textProperty().addListener((obs, oldValue, newValue) -> refreshMatchLabel.run());
        replaceFindField.textProperty().addListener((obs, oldValue, newValue) -> refreshMatchLabel.run());
        findPrevious.setOnAction(event -> {
            navigateSearchFromDialog(findField.getText(), -1);
            refreshMatchLabel.run();
        });
        findNext.setOnAction(event -> {
            navigateSearchFromDialog(findField.getText(), 1);
            refreshMatchLabel.run();
        });
        replacePrevious.setOnAction(event -> {
            replaceNavigatedSearchResult(replaceFindField.getText(), replaceField.getText(), -1);
            refreshMatchLabel.run();
        });
        replaceCurrent.setOnAction(event -> {
            replaceCurrentSearchResult(replaceFindField.getText(), replaceField.getText());
            refreshMatchLabel.run();
        });
        replaceNext.setOnAction(event -> {
            replaceNavigatedSearchResult(replaceFindField.getText(), replaceField.getText(), 1);
            refreshMatchLabel.run();
        });
        replaceAll.setOnAction(event -> {
            replaceAllSearchResults(replaceFindField.getText(), replaceField.getText());
            refreshMatchLabel.run();
        });

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                new Tab("查找(D)", findDialogContent(findField, findMatchLabel, findPrevious, findNext)),
                new Tab("替换(P)", replaceDialogContent(replaceFindField, replaceField, replaceMatchLabel,
                        replacePrevious, replaceCurrent, replaceNext, replaceAll))
        );
        tabs.getSelectionModel().selectedIndexProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.intValue() == 0) {
                findField.setText(replaceFindField.getText());
            } else {
                replaceFindField.setText(findField.getText());
            }
        });
        dialog.getDialogPane().setContent(tabs);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.setOnHidden(event -> search(""));
        Platform.runLater(findField::requestFocus);
        dialog.show();
    }

    private javafx.scene.Node findDialogContent(TextField findField, Label matchLabel, Button findPrevious,
                                                Button findNext) {
        javafx.scene.layout.GridPane pane = new javafx.scene.layout.GridPane();
        pane.setHgap(12);
        pane.setVgap(14);
        pane.setPadding(new Insets(14));
        pane.add(new Label("查找内容(N)"), 0, 0);
        pane.add(findField, 1, 0, 4, 1);
        pane.add(new Label("选项:"), 0, 1);
        pane.add(new Label("向下, 区分全/半角"), 1, 1, 4, 1);
        pane.add(matchLabel, 0, 2, 2, 1);
        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10, findPrevious, findNext);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        pane.add(actions, 2, 2, 3, 1);
        return pane;
    }

    private javafx.scene.Node replaceDialogContent(TextField findField, TextField replaceField, Label matchLabel,
                                                   Button replacePrevious, Button replaceCurrent,
                                                   Button replaceNext, Button replaceAll) {
        javafx.scene.layout.GridPane pane = new javafx.scene.layout.GridPane();
        pane.setHgap(12);
        pane.setVgap(14);
        pane.setPadding(new Insets(14));
        pane.add(new Label("查找内容(N)"), 0, 0);
        pane.add(findField, 1, 0, 4, 1);
        pane.add(new Label("替换为(I)"), 0, 1);
        pane.add(replaceField, 1, 1, 4, 1);
        pane.add(new Label("选项:"), 0, 2);
        pane.add(new Label("向下, 区分全/半角"), 1, 2, 4, 1);
        pane.add(matchLabel, 0, 3, 2, 1);
        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10,
                replacePrevious, replaceCurrent, replaceNext, replaceAll);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        pane.add(actions, 1, 3, 4, 1);
        return pane;
    }

    public void replaceCurrentSearchResult(String keyword, String replacement) {
        String target = keyword == null ? "" : keyword;
        if (target.isBlank()) {
            notice("请输入查找内容");
            return;
        }
        if (searchState.isEmpty() || !searchState.matchesKeyword(target)) {
            search(target);
        }
        if (searchState.isEmpty()) {
            notice("没有可替换的搜索结果");
            return;
        }
        String id = searchState.currentOrFirst();
        executeAndRefresh(new RenameNodeCommand(currentMap, () ->
                find(id).ifPresent(node -> node.setText(SearchState.replaceFirstIgnoreCase(node.getText(), target, replacement)))),
                "已替换当前匹配项", true);
        search(target);
    }

    public void replaceAllSearchResults(String keyword, String replacement) {
        String target = keyword == null ? "" : keyword;
        if (target.isBlank()) {
            notice("请输入查找内容");
            return;
        }
        List<String> ids = searchService.search(currentMap, target).stream().map(MindNode::getId).toList();
        if (ids.isEmpty()) {
            notice("没有可替换的搜索结果");
            return;
        }
        executeAndRefresh(new RenameNodeCommand(currentMap, () -> {
            for (String id : ids) {
                find(id).ifPresent(node -> node.setText(SearchState.replaceAllIgnoreCase(node.getText(), target, replacement)));
            }
        }), "已替换 " + ids.size() + " 个节点", true);
        search(target);
    }

    public void replaceNavigatedSearchResult(String keyword, String replacement, int delta) {
        if (!ensureSearchReady(keyword)) {
            return;
        }
        navigateSearch(delta);
        replaceCurrentSearchResult(keyword, replacement);
    }

    private void navigateSearch(int delta) {
        if (searchState.isEmpty()) {
            notice("没有搜索结果");
            return;
        }
        String id = searchState.navigate(delta);
        expandAncestorsIfNeeded(id);
        selectionModel.setPrimaryNodeId(id);
        refreshAll("定位到搜索结果 " + (searchState.currentIndex() + 1) + "/" + searchState.size(), true);
        Platform.runLater(() -> mainFrame.getCanvas().scrollToNode(id));
    }

    private void navigateSearchFromDialog(String keyword, int delta) {
        if (ensureSearchReady(keyword)) {
            navigateSearch(delta);
        }
    }

    private boolean ensureSearchReady(String keyword) {
        String target = keyword == null ? "" : keyword;
        if (target.isBlank()) {
            notice("请输入查找内容");
            return false;
        }
        if (searchState.isEmpty() || !searchState.matchesKeyword(target)) {
            search(target);
        }
        if (searchState.isEmpty()) {
            notice("没有搜索结果");
            return false;
        }
        return true;
    }

    public void zoomIn() {
        currentMap.setZoom(currentMap.getZoom() + 0.1);
        refreshAll("缩放比例：" + Math.round(currentMap.getZoom() * 100) + "%", false);
    }

    public void zoomOut() {
        currentMap.setZoom(currentMap.getZoom() - 0.1);
        refreshAll("缩放比例：" + Math.round(currentMap.getZoom() * 100) + "%", false);
    }

    public void resetZoom() {
        currentMap.setZoom(1.0);
        refreshAll("缩放比例：100%", false);
    }

    public void changeZoom(double zoom) {
        currentMap.setZoom(zoom);
        refreshAll("缩放比例：" + Math.round(currentMap.getZoom() * 100) + "%", false);
    }

    public void fitToWindow() {
        double zoomX = mainFrame.getCanvas().getViewportBounds().getWidth() / Math.max(1, preferredCanvasWidth());
        double zoomY = mainFrame.getCanvas().getViewportBounds().getHeight() / Math.max(1, preferredCanvasHeight());
        currentMap.setZoom(Math.min(1.6, Math.max(0.3, Math.min(zoomX, zoomY) * 0.92)));
        refreshAll("已适应窗口", false);
    }

    public void selectNode(MindNode node, boolean toggle) {
        MindNode live = find(node.getId()).orElse(node);
        if (toggle) {
            selectionModel.toggle(live);
        } else {
            selectionModel.selectOnly(live);
        }
        refreshAll("已选中节点：" + live.getText(), false);
    }

    public void selectConnection(String childNodeId, boolean toggle) {
        MindNode child = find(childNodeId).orElse(null);
        if (child == null || child.getParent() == null) {
            return;
        }
        if (toggle) {
            selectionModel.toggleConnection(childNodeId);
        } else {
            selectionModel.selectOnlyConnection(childNodeId);
        }
        refreshAll("已选中连线：" + child.getParent().getText() + " → " + child.getText(), false);
    }

    public void prepareNodeForDrag(MindNode node, boolean toggle) {
        MindNode live = find(node.getId()).orElse(node);
        if (!selectionModel.contains(live)) {
            if (toggle) {
                selectionModel.toggle(live);
            } else {
                selectionModel.selectOnly(live);
            }
        }
    }

    public void prepareConnectionForContext(String childNodeId) {
        MindNode child = find(childNodeId).orElse(null);
        if (child != null && child.getParent() != null && !selectionModel.containsConnection(childNodeId)) {
            selectionModel.selectOnlyConnection(childNodeId);
        }
    }

    public void selectFromTree(String nodeId) {
        find(nodeId).ifPresent(node -> {
            selectionModel.selectOnly(node);
            refreshAll("已从结构树选中：" + node.getText(), false);
            Platform.runLater(() -> mainFrame.getCanvas().scrollToNode(nodeId));
        });
    }

    public void clearSelection() {
        selectionModel.clear();
        refreshAll("已取消选择", false);
    }

    public void selectAllItems() {
        List<MindNode> nodes = currentMap.allNodes();
        List<String> connectionIds = nodes.stream()
                .filter(node -> node.getParent() != null)
                .map(MindNode::getId)
                .toList();
        selectionModel.selectAll(nodes, connectionIds);
        refreshAll("已全选 " + nodes.size() + " 个节点、" + connectionIds.size() + " 条连线", false);
    }

    public void selectItemsInArea(double minX, double minY, double maxX, double maxY) {
        List<MindNode> nodes = currentMap.visibleNodes().stream()
                .filter(node -> GeometryUtils.intersectsNode(node, minX, minY, maxX, maxY))
                .toList();
        Set<String> visibleIds = currentMap.visibleNodes().stream()
                .map(MindNode::getId)
                .collect(java.util.stream.Collectors.toSet());
        List<String> connectionIds = currentMap.visibleNodes().stream()
                .filter(node -> node.getParent() != null && visibleIds.contains(node.getParent().getId()))
                .filter(node -> GeometryUtils.intersectsConnection(node, minX, minY, maxX, maxY))
                .map(MindNode::getId)
                .toList();
        selectionModel.selectAll(nodes, connectionIds);
        refreshAll(selectionAreaStatus(nodes.size(), connectionIds.size()), false);
    }

    public boolean isSelected(MindNode node) {
        return selectionModel.contains(node);
    }

    public void beginDragSnapshot() {
        dragBefore = currentMap.deepCopy();
    }

    public void dragSelectedBy(double dx, double dy) {
        for (MindNode node : topLevelSelected(selectedNodes())) {
            find(node.getId()).ifPresent(live -> live.moveBy(dx, dy));
        }
        mainFrame.setStatus("正在拖拽节点");
    }

    public void finishDragSnapshot() {
        if (dragBefore == null) {
            return;
        }
        MindMap after = currentMap.deepCopy();
        if (!GeometryUtils.geometryChanged(dragBefore, after)) {
            dragBefore = null;
            return;
        }
        editHistory.pushExecuted(new MoveNodeCommand(currentMap, dragBefore, after));
        currentMap.setModified(true);
        dragBefore = null;
        refreshAll("已移动节点", false);
    }

    public void resizeNode(String nodeId, double x, double y, double width, double height) {
        find(nodeId).ifPresent(node -> {
            double oldX = node.getX();
            double oldY = node.getY();
            node.setX(x);
            node.setY(y);
            node.setOffsetX(node.getOffsetX() + node.getX() - oldX);
            node.setOffsetY(node.getOffsetY() + node.getY() - oldY);
            node.setWidth(width);
            node.setHeight(height);
        });
    }

    public double preferredCanvasWidth() {
        return layoutService.preferredWidth(currentMap);
    }

    public double preferredCanvasHeight() {
        return layoutService.preferredHeight(currentMap);
    }

    public void installShortcuts(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), this::newMap);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), this::openMap);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::saveMap);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), this::saveMapAs);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN), this::exportPng);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN), this::undo);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), this::redo);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN), this::redo);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DELETE), this::deleteSelectedNodes);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F2), this::renameSelectedNode);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F3), this::nextSearchResult);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F3, KeyCombination.SHIFT_DOWN), this::previousSearchResult);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), this::showFindReplaceDialog);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN), this::showFindReplaceDialog);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.PLUS, KeyCombination.CONTROL_DOWN), this::zoomIn);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN), this::zoomIn);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN), this::zoomOut);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN), this::resetZoom);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN), this::fitToWindow);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::clearSelection);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (scene.getFocusOwner() instanceof TextInputControl) {
                return;
            }
            if (event.isControlDown() && event.getCode() == KeyCode.A) {
                selectAllItems();
                event.consume();
            } else if (event.getCode() == KeyCode.TAB) {
                addChildNode();
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                addSiblingNode();
                event.consume();
            }
        });
    }

    private void executeAndRefresh(Command command, String status, boolean relayout) {
        editHistory.execute(command);
        refreshAll(status, relayout);
    }

    private void refreshAll(String status, boolean relayout) {
        if (relayout) {
            layoutService.layout(currentMap);
        }
        Set<String> searchIds = new HashSet<>(searchState.ids());
        selectionModel.getSelectedNodeIds().removeIf(id -> find(id).isEmpty());
        selectionModel.getSelectedConnectionIds().removeIf(id -> find(id).map(MindNode::getParent).isEmpty());
        if (selectionModel.getPrimaryNodeId() != null && find(selectionModel.getPrimaryNodeId()).isEmpty()) {
            selectionModel.selectOnly(currentMap.getRoot());
        }
        mainFrame.getCanvas().refresh(currentMap, selectionModel.getSelectedNodeIds(),
                selectionModel.getSelectedConnectionIds(), searchIds, searchState.keyword());
        mainFrame.getTreePanel().refresh(currentMap, selectionModel.getPrimaryNodeId());
        mainFrame.getToolbarPanel().updateState(primaryNode() != null,
                primaryNode() != null && !primaryNode().isRoot(),
                selectedNodes().stream().anyMatch(node -> !node.isRoot()),
                primaryNode() != null && !primaryNode().getChildren().isEmpty(),
                editHistory.canUndo(),
                editHistory.canRedo(),
                currentMap.getLayoutType());
        updateToolbarNodeStyle();
        mainFrame.getToolbarPanel().updateCanvasColor(currentMap.getCanvasColor());
        ConnectionStyle connectionStyle = activeConnectionStyle();
        mainFrame.getToolbarPanel().updateConnectionStyle(connectionStyle.getColor(),
                connectionStyle.getWidth(), connectionStyle.isDashed(), connectionStyle.getShape());
        refreshRecentFiles();
        mainFrame.setStatus(status);
        mainFrame.setZoom(currentMap.getZoom());
        mainFrame.setDocumentStats(countWords(), currentMap.allNodes().size());
        updateTitle();
    }

    private void refreshRecentFiles() {
        mainFrame.getToolbarPanel().updateRecentFiles(recentFileService.load());
    }

    private void updateTitle() {
        String name = currentMap.getFilePath() == null ? currentMap.getName() : currentMap.getFilePath().getFileName().toString();
        stage.setTitle("思维导图绘制工具 - " + name + (currentMap.isModified() ? "*" : ""));
    }

    private void notice(String message) {
        mainFrame.setStatus(message);
    }

    private void updateToolbarNodeStyle() {
        MindNode node = primaryNode();
        if (node == null) {
            mainFrame.getToolbarPanel().updateNodeStyle(Color.WHITE, Color.web("#CBD5E1"), Color.web("#0F172A"));
            mainFrame.getToolbarPanel().updateTextStyle("Microsoft YaHei UI", 13,
                    false, false, false, false);
            return;
        }
        com.example.mindmap.model.NodeStyle style = node.getStyle();
        mainFrame.getToolbarPanel().updateNodeStyle(style.getFillColor(), style.getBorderColor(), style.getTextColor());
        mainFrame.getToolbarPanel().updateTextStyle(style.getFontFamily(), style.getFontSize(),
                style.isBold(), style.isItalic(),
                style.isUnderline(), style.isStrikethrough());
    }

    private int countWords() {
        return currentMap.allNodes().stream()
                .map(MindNode::getText)
                .filter(java.util.Objects::nonNull)
                .mapToInt(text -> (int) text.codePoints()
                        .filter(codePoint -> !Character.isWhitespace(codePoint))
                        .count())
                .sum();
    }

    private boolean confirmBeforeDestructiveAction() {
        if (!currentMap.isModified()) {
            return true;
        }
        Dialogs.SaveChoice choice = Dialogs.confirmSave(stage);
        if (choice == Dialogs.SaveChoice.CANCEL) {
            return false;
        }
        return choice == Dialogs.SaveChoice.DISCARD || saveMap();
    }

    private MindNode primaryNode() {
        return find(selectionModel.getPrimaryNodeId()).orElse(null);
    }

    private List<MindNode> selectedNodes() {
        return selectionModel.getSelectedNodeIds().stream()
                .map(this::find)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<MindNode> find(String id) {
        return currentMap.findNodeById(id);
    }

    private List<MindNode> topLevelSelected(List<MindNode> nodes) {
        Set<String> selectedIds = nodes.stream().map(MindNode::getId).collect(java.util.stream.Collectors.toSet());
        return nodes.stream()
                .filter(node -> {
                    MindNode parent = node.getParent();
                    while (parent != null) {
                        if (selectedIds.contains(parent.getId())) {
                            return false;
                        }
                        parent = parent.getParent();
                    }
                    return true;
                })
                .sorted(Comparator.comparing(MindNode::getId))
                .toList();
    }

    private void expandAncestorsIfNeeded(String id) {
        find(id).ifPresent(node -> {
            MindNode parent = node.getParent();
            while (parent != null) {
                if (parent.isCollapsed()) {
                    parent.setCollapsed(false);
                }
                parent = parent.getParent();
            }
        });
    }

    private String selectionAreaStatus(int nodeCount, int connectionCount) {
        if (nodeCount == 0 && connectionCount == 0) {
            return "框选范围内没有对象";
        }
        if (nodeCount == 0) {
            return "已框选 " + connectionCount + " 条连线";
        }
        if (connectionCount == 0) {
            return "已框选 " + nodeCount + " 个节点";
        }
        return "已框选 " + nodeCount + " 个节点、" + connectionCount + " 条连线";
    }

    private FileChooser mindMapChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("思维导图文件", "*.mindmap"));
        return chooser;
    }

    private Path showOpen(FileChooser chooser) {
        var file = chooser.showOpenDialog(stage);
        return file == null ? null : file.toPath();
    }

    private Path showSave(FileChooser chooser) {
        var file = chooser.showSaveDialog(stage);
        return file == null ? null : file.toPath();
    }

    private Path ensureExtension(Path path, String extension) {
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(extension)) {
            return path;
        }
        return path.resolveSibling(path.getFileName() + extension);
    }
}
