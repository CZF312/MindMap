package com.example.mindmap.controller;

import com.example.mindmap.command.AddNodeCommand;
import com.example.mindmap.command.ChangeLayoutCommand;
import com.example.mindmap.command.ChangeStyleCommand;
import com.example.mindmap.command.Command;
import com.example.mindmap.command.DeleteNodeCommand;
import com.example.mindmap.command.MoveNodeCommand;
import com.example.mindmap.command.RenameNodeCommand;
import com.example.mindmap.command.ToggleCollapseCommand;
import com.example.mindmap.model.LayoutType;
import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;
import com.example.mindmap.model.SelectionModel;
import com.example.mindmap.service.ImageExportService;
import com.example.mindmap.service.MindMapFileService;
import com.example.mindmap.service.MindMapLayoutService;
import com.example.mindmap.service.RecentFileService;
import com.example.mindmap.service.SearchService;
import com.example.mindmap.util.Dialogs;
import com.example.mindmap.view.MainFrame;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
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
    private final ArrayDeque<Command> undoStack = new ArrayDeque<>();
    private final ArrayDeque<Command> redoStack = new ArrayDeque<>();
    private final List<String> searchResultIds = new ArrayList<>();
    private MindMap currentMap = new MindMap();
    private MindMap dragBefore;
    private int searchIndex = -1;

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
        undoStack.clear();
        redoStack.clear();
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
            undoStack.clear();
            redoStack.clear();
            recentFileService.add(path);
            refreshAll((recent ? "已打开最近文件：" : "打开成功：") + path, true);
        } catch (Exception ex) {
            Dialogs.error(stage, "打开失败", ex.getMessage());
            refreshAll("打开失败，当前导图未改变", true);
        }
    }

    public boolean saveMap() {
        if (currentMap.getFilePath() == null) {
            return saveMapAs();
        }
        return saveTo(currentMap.getFilePath());
    }

    public boolean saveMapAs() {
        FileChooser chooser = mindMapChooser("保存思维导图");
        Path path = showSave(chooser);
        if (path == null) {
            mainFrame.setStatus("已取消保存");
            return false;
        }
        return saveTo(ensureExtension(path, ".mindmap"));
    }

    private boolean saveTo(Path path) {
        try {
            fileService.save(currentMap, path);
            recentFileService.add(path);
            refreshAll("保存成功：" + path, true);
            return true;
        } catch (Exception ex) {
            Dialogs.error(stage, "保存失败", ex.getMessage());
            mainFrame.setStatus("保存失败");
            return false;
        }
    }

    public void exportPng() {
        exportImage("导出 PNG 图片", ".png", true);
    }

    public void exportJpg() {
        exportImage("导出 JPG 图片", ".jpg", false);
    }

    private void exportImage(String title, String extension, boolean png) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extension.toUpperCase() + " 图片", "*" + extension));
        Path path = showSave(chooser);
        if (path == null) {
            mainFrame.setStatus("已取消导出");
            return;
        }
        path = ensureExtension(path, extension);
        try {
            if (png) {
                imageExportService.exportPng(mainFrame.getCanvas(), path);
            } else {
                imageExportService.exportJpg(mainFrame.getCanvas(), path);
            }
            mainFrame.setStatus("导出成功：" + path);
        } catch (Exception ex) {
            Dialogs.error(stage, "导出失败", ex.getMessage());
            mainFrame.setStatus("导出失败");
        }
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
                .ifPresent(text -> executeAndRefresh(new RenameNodeCommand(currentMap, () -> {
                    find(node.getId()).ifPresent(live -> live.setText(text));
                    selectionModel.setPrimaryNodeId(node.getId());
                }), "已重命名节点", true));
    }

    public void changeFillColor(Color color) {
        changeSelectedStyle(color, true);
    }

    public void changeBorderColor(Color color) {
        changeSelectedStyle(color, false);
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
        if (undoStack.isEmpty()) {
            notice("没有可撤销操作");
            return;
        }
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        refreshAll("已撤销", true);
    }

    public void redo() {
        if (redoStack.isEmpty()) {
            notice("没有可重做操作");
            return;
        }
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        refreshAll("已重做", true);
    }

    public void search(String keyword) {
        searchResultIds.clear();
        searchResultIds.addAll(searchService.search(currentMap, keyword).stream().map(MindNode::getId).toList());
        searchIndex = searchResultIds.isEmpty() ? -1 : 0;
        refreshAll(searchResultIds.isEmpty()
                ? (keyword == null || keyword.isBlank() ? "已清空搜索" : "未找到匹配节点")
                : "搜索到 " + searchResultIds.size() + " 个匹配节点", false);
    }

    public void nextSearchResult() {
        navigateSearch(1);
    }

    public void previousSearchResult() {
        navigateSearch(-1);
    }

    private void navigateSearch(int delta) {
        if (searchResultIds.isEmpty()) {
            notice("没有搜索结果");
            return;
        }
        searchIndex = Math.floorMod(searchIndex + delta, searchResultIds.size());
        String id = searchResultIds.get(searchIndex);
        expandAncestorsIfNeeded(id);
        selectionModel.setPrimaryNodeId(id);
        refreshAll("定位到搜索结果 " + (searchIndex + 1) + "/" + searchResultIds.size(), true);
        Platform.runLater(() -> mainFrame.getCanvas().scrollToNode(id));
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
        refreshAll("正在拖拽节点", false);
    }

    public void finishDragSnapshot() {
        if (dragBefore == null) {
            return;
        }
        MindMap after = currentMap.deepCopy();
        undoStack.push(new MoveNodeCommand(currentMap, dragBefore, after));
        redoStack.clear();
        currentMap.setModified(true);
        dragBefore = null;
        refreshAll("已移动节点", false);
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
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN), this::redo);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DELETE), this::deleteSelectedNodes);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F2), this::renameSelectedNode);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), () -> mainFrame.getToolbarPanel().getSearchField().requestFocus());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.PLUS, KeyCombination.CONTROL_DOWN), this::zoomIn);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN), this::zoomIn);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN), this::zoomOut);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN), this::resetZoom);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN), this::fitToWindow);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (scene.getFocusOwner() instanceof TextInputControl) {
                return;
            }
            if (event.getCode() == KeyCode.TAB) {
                addChildNode();
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                addSiblingNode();
                event.consume();
            }
        });
    }

    private void executeAndRefresh(Command command, String status, boolean relayout) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();
        refreshAll(status, relayout);
    }

    private void refreshAll(String status, boolean relayout) {
        if (relayout) {
            layoutService.layout(currentMap);
        }
        Set<String> searchIds = new HashSet<>(searchResultIds);
        selectionModel.getSelectedNodeIds().removeIf(id -> find(id).isEmpty());
        if (selectionModel.getPrimaryNodeId() != null && find(selectionModel.getPrimaryNodeId()).isEmpty()) {
            selectionModel.selectOnly(currentMap.getRoot());
        }
        mainFrame.getCanvas().refresh(currentMap, selectionModel.getSelectedNodeIds(), searchIds);
        mainFrame.getTreePanel().refresh(currentMap, selectionModel.getPrimaryNodeId());
        mainFrame.getToolbarPanel().updateState(primaryNode() != null,
                primaryNode() != null && !primaryNode().isRoot(),
                selectedNodes().stream().anyMatch(node -> !node.isRoot()),
                primaryNode() != null && !primaryNode().getChildren().isEmpty(),
                !undoStack.isEmpty(),
                !redoStack.isEmpty(),
                currentMap.getLayoutType());
        refreshRecentFiles();
        mainFrame.setStatus(status);
        mainFrame.setZoom(currentMap.getZoom());
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
            boolean changed = false;
            while (parent != null) {
                if (parent.isCollapsed()) {
                    parent.setCollapsed(false);
                    changed = true;
                }
                parent = parent.getParent();
            }
            if (changed) {
                currentMap.setModified(true);
            }
        });
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
