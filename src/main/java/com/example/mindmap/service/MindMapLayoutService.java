package com.example.mindmap.service;

import com.example.mindmap.model.LayoutType;
import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;

import java.util.ArrayList;
import java.util.List;

public class MindMapLayoutService {
    private static final double ROOT_X = 860;
    private static final double ROOT_Y = 520;
    private static final double H_GAP = 230;
    private static final double V_GAP = 38;
    private static final double MIN_SUBTREE_HEIGHT = 82;
    private static final double MIN_CANVAS = 1400;

    public void layout(MindMap map) {
        if (map == null || map.getRoot() == null) {
            return;
        }
        map.allNodes().forEach(this::measureNode);
        MindNode root = map.getRoot();
        root.setX(ROOT_X - root.getWidth() / 2 + root.getOffsetX());
        root.setY(ROOT_Y - root.getHeight() / 2 + root.getOffsetY());

        if (map.getLayoutType() == LayoutType.LEFT) {
            layoutSide(root.getChildren(), -1, root.getX() - H_GAP, ROOT_Y, true);
        } else if (map.getLayoutType() == LayoutType.RIGHT) {
            layoutSide(root.getChildren(), 1, root.getX() + root.getWidth() + H_GAP, ROOT_Y, true);
        } else {
            List<MindNode> left = new ArrayList<>();
            List<MindNode> right = new ArrayList<>();
            for (int i = 0; i < root.getChildren().size(); i++) {
                (i % 2 == 0 ? right : left).add(root.getChildren().get(i));
            }
            layoutSide(right, 1, root.getX() + root.getWidth() + H_GAP, ROOT_Y, true);
            layoutSide(left, -1, root.getX() - H_GAP, ROOT_Y, true);
        }
        shiftIntoPositiveArea(map);
    }

    private void measureNode(MindNode node) {
        String text = node.getText() == null ? "" : node.getText();
        int visualLength = text.codePointCount(0, text.length());
        double width = Math.min(230, Math.max(node.isRoot() ? 150 : 126, 46 + visualLength * 9.2));
        int lines = Math.max(1, (int) Math.ceil((visualLength * 9.2) / Math.max(80, width - 34)));
        node.setWidth(width);
        node.setHeight(Math.max(node.isRoot() ? 58 : 48, 30 + lines * 20));
    }

    private void layoutSide(List<MindNode> nodes, int direction, double anchorX, double centerY, boolean firstLevel) {
        if (nodes.isEmpty()) {
            return;
        }
        double totalHeight = totalHeight(nodes);
        double cursor = centerY - totalHeight / 2.0;
        for (MindNode node : nodes) {
            double subtreeHeight = subtreeHeight(node);
            double y = cursor + subtreeHeight / 2.0 - node.getHeight() / 2.0;
            double x = direction > 0 ? anchorX : anchorX - node.getWidth();
            node.setX(x + node.getOffsetX());
            node.setY(y + node.getOffsetY());
            if (!node.isCollapsed()) {
                double childAnchor = direction > 0
                        ? node.getX() + node.getWidth() + H_GAP
                        : node.getX() - H_GAP;
                layoutSide(node.getChildren(), direction, childAnchor, node.getCenterY(), false);
            }
            cursor += subtreeHeight + (firstLevel ? V_GAP * 1.25 : V_GAP);
        }
    }

    private double totalHeight(List<MindNode> nodes) {
        double total = 0;
        for (MindNode node : nodes) {
            total += subtreeHeight(node);
        }
        total += Math.max(0, nodes.size() - 1) * V_GAP;
        return total;
    }

    private double subtreeHeight(MindNode node) {
        if (node.isCollapsed() || node.getChildren().isEmpty()) {
            return Math.max(MIN_SUBTREE_HEIGHT, node.getHeight());
        }
        return Math.max(MIN_SUBTREE_HEIGHT, Math.max(node.getHeight(), totalHeight(node.getChildren())));
    }

    private void shiftIntoPositiveArea(MindMap map) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        for (MindNode node : map.visibleNodes()) {
            minX = Math.min(minX, node.getX());
            minY = Math.min(minY, node.getY());
        }
        double dx = minX < 80 ? 80 - minX : 0;
        double dy = minY < 80 ? 80 - minY : 0;
        if (dx == 0 && dy == 0) {
            return;
        }
        for (MindNode node : map.visibleNodes()) {
            node.setX(node.getX() + dx);
            node.setY(node.getY() + dy);
        }
    }

    public double preferredWidth(MindMap map) {
        double max = MIN_CANVAS;
        for (MindNode node : map.visibleNodes()) {
            max = Math.max(max, node.getX() + node.getWidth() + 180);
        }
        return max;
    }

    public double preferredHeight(MindMap map) {
        double max = MIN_CANVAS * 0.7;
        for (MindNode node : map.visibleNodes()) {
            max = Math.max(max, node.getY() + node.getHeight() + 160);
        }
        return max;
    }
}
