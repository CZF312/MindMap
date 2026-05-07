package com.example.mindmap.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MindNode {
    private final String id;
    private String text;
    private MindNode parent;
    private final List<MindNode> children = new ArrayList<>();
    private double x;
    private double y;
    private double width = 150;
    private double height = 54;
    private double offsetX;
    private double offsetY;
    private boolean collapsed;
    private NodeStyle style = new NodeStyle();

    public MindNode(String id, String text) {
        this.id = Objects.requireNonNull(id);
        this.text = text == null || text.isBlank() ? "新节点" : text;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text == null || text.isBlank() ? "新节点" : text.trim();
    }

    public MindNode getParent() {
        return parent;
    }

    public void setParent(MindNode parent) {
        this.parent = parent;
    }

    public List<MindNode> getChildren() {
        return children;
    }

    public void addChild(MindNode child) {
        child.setParent(this);
        children.add(child);
    }

    public void addChild(int index, MindNode child) {
        child.setParent(this);
        children.add(Math.max(0, Math.min(index, children.size())), child);
    }

    public void removeChild(MindNode child) {
        children.remove(child);
        child.setParent(null);
    }

    public boolean isRoot() {
        return parent == null;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getCenterX() {
        return x + width / 2.0;
    }

    public double getCenterY() {
        return y + height / 2.0;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }

    public void moveBy(double dx, double dy) {
        x += dx;
        y += dy;
        offsetX += dx;
        offsetY += dy;
        for (MindNode child : children) {
            child.moveBy(dx, dy);
        }
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public NodeStyle getStyle() {
        return style;
    }

    public void setStyle(NodeStyle style) {
        this.style = style == null ? new NodeStyle() : style;
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    public List<MindNode> descendantsAndSelf() {
        List<MindNode> nodes = new ArrayList<>();
        collect(this, nodes);
        return nodes;
    }

    private static void collect(MindNode node, List<MindNode> nodes) {
        nodes.add(node);
        for (MindNode child : node.children) {
            collect(child, nodes);
        }
    }

    public MindNode deepCopy() {
        MindNode copy = new MindNode(id, text);
        copy.x = x;
        copy.y = y;
        copy.width = width;
        copy.height = height;
        copy.offsetX = offsetX;
        copy.offsetY = offsetY;
        copy.collapsed = collapsed;
        copy.style = style.copy();
        for (MindNode child : children) {
            copy.addChild(child.deepCopy());
        }
        return copy;
    }
}
