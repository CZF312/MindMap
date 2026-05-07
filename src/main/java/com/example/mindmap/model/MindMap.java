package com.example.mindmap.model;

import javafx.scene.paint.Color;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public class MindMap {
    private String name = "未命名";
    private MindNode root;
    private LayoutType layoutType = LayoutType.AUTO;
    private Path filePath;
    private boolean modified;
    private double zoom = 1.0;
    private Color canvasColor = Color.WHITE;
    private long nextId = 1;

    public MindMap() {
        root = new MindNode(nextNodeId(), "中心主题");
        root.getStyle().setFillColor(javafx.scene.paint.Color.web("#2563EB"));
        root.getStyle().setBorderColor(javafx.scene.paint.Color.web("#1D4ED8"));
        root.getStyle().setTextColor(javafx.scene.paint.Color.WHITE);
    }

    public String nextNodeId() {
        return "node-" + nextId++;
    }

    public MindNode createNode(String text) {
        return new MindNode(nextNodeId(), text);
    }

    public Optional<MindNode> findNodeById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        Deque<MindNode> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            MindNode node = stack.pop();
            if (id.equals(node.getId())) {
                return Optional.of(node);
            }
            for (MindNode child : node.getChildren()) {
                stack.push(child);
            }
        }
        return Optional.empty();
    }

    public List<MindNode> allNodes() {
        return root == null ? List.of() : root.descendantsAndSelf();
    }

    public List<MindNode> visibleNodes() {
        List<MindNode> nodes = new ArrayList<>();
        collectVisible(root, nodes);
        return nodes;
    }

    private void collectVisible(MindNode node, List<MindNode> nodes) {
        if (node == null) {
            return;
        }
        nodes.add(node);
        if (!node.isCollapsed()) {
            for (MindNode child : node.getChildren()) {
                collectVisible(child, nodes);
            }
        }
    }

    public MindMap deepCopy() {
        MindMap copy = new MindMap();
        copy.name = name;
        copy.root = root == null ? null : root.deepCopy();
        copy.layoutType = layoutType;
        copy.filePath = filePath;
        copy.modified = modified;
        copy.zoom = zoom;
        copy.canvasColor = canvasColor;
        copy.nextId = nextId;
        return copy;
    }

    public void copyFrom(MindMap other) {
        name = other.name;
        root = other.root == null ? null : other.root.deepCopy();
        layoutType = other.layoutType;
        filePath = other.filePath;
        modified = other.modified;
        zoom = other.zoom;
        canvasColor = other.canvasColor;
        nextId = other.nextId;
    }

    public void bumpNextIdFromExistingNodes() {
        long max = 0;
        for (MindNode node : allNodes()) {
            String id = node.getId();
            int dash = id.lastIndexOf('-');
            if (dash >= 0) {
                try {
                    max = Math.max(max, Long.parseLong(id.substring(dash + 1)));
                } catch (NumberFormatException ignored) {
                    // Keep scanning other ids.
                }
            }
        }
        nextId = Math.max(nextId, max + 1);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null || name.isBlank() ? "未命名" : name.trim();
    }

    public MindNode getRoot() {
        return root;
    }

    public void setRoot(MindNode root) {
        this.root = root;
    }

    public LayoutType getLayoutType() {
        return layoutType;
    }

    public void setLayoutType(LayoutType layoutType) {
        this.layoutType = layoutType == null ? LayoutType.AUTO : layoutType;
    }

    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom = Math.max(0.3, Math.min(3.0, zoom));
    }

    public Color getCanvasColor() {
        return canvasColor;
    }

    public void setCanvasColor(Color canvasColor) {
        this.canvasColor = canvasColor == null ? Color.WHITE : canvasColor;
    }
}
