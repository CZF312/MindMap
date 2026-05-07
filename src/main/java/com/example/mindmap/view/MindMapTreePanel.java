package com.example.mindmap.view;

import com.example.mindmap.controller.MindMapController;
import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class MindMapTreePanel extends BorderPane {
    private final MindMapController controller;
    private final TreeView<NodeRef> treeView = new TreeView<>();
    private boolean refreshing;

    public MindMapTreePanel(MindMapController controller, Runnable collapseAction) {
        this.controller = controller;
        getStyleClass().add("tree-panel");

        Label title = new Label("结构");
        title.getStyleClass().add("side-title");
        title.setMaxWidth(Double.MAX_VALUE);
        Button collapseButton = new Button(">");
        collapseButton.getStyleClass().add("side-toggle-button");
        collapseButton.setTooltip(new Tooltip("收起结构栏"));
        collapseButton.setOnAction(event -> collapseAction.run());
        HBox header = new HBox(8, title, collapseButton);
        header.getStyleClass().add("tree-panel-header");
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);

        treeView.getStyleClass().add("structure-tree");
        treeView.setShowRoot(true);
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (!refreshing && newItem != null) {
                controller.selectFromTree(newItem.getValue().id());
            }
        });
        setTop(header);
        setCenter(treeView);
    }

    public void refresh(MindMap map, String selectedNodeId) {
        refreshing = true;
        if (map == null || map.getRoot() == null) {
            treeView.setRoot(null);
            refreshing = false;
            return;
        }
        TreeItem<NodeRef> root = build(map.getRoot());
        root.setExpanded(true);
        treeView.setRoot(root);
        selectItem(root, selectedNodeId);
        refreshing = false;
    }

    private TreeItem<NodeRef> build(MindNode node) {
        String label = node.getText() + (node.isCollapsed() ? " [已折叠]" : "");
        TreeItem<NodeRef> item = new TreeItem<>(new NodeRef(node.getId(), label));
        item.setExpanded(true);
        for (MindNode child : node.getChildren()) {
            item.getChildren().add(build(child));
        }
        return item;
    }

    private boolean selectItem(TreeItem<NodeRef> item, String selectedNodeId) {
        if (item.getValue().id().equals(selectedNodeId)) {
            treeView.getSelectionModel().select(item);
            return true;
        }
        for (TreeItem<NodeRef> child : item.getChildren()) {
            if (selectItem(child, selectedNodeId)) {
                return true;
            }
        }
        return false;
    }

    private record NodeRef(String id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
