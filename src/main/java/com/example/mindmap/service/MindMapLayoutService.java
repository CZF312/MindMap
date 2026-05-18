package com.example.mindmap.service;

import com.example.mindmap.model.LayoutType;
import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class MindMapLayoutService {
    private static final double ROOT_X = 860;
    private static final double ROOT_Y = 520;
    private static final double H_GAP = 230;
    private static final double V_GAP = 38;
    private static final double MIN_SUBTREE_HEIGHT = 82;
    private static final double MIN_CANVAS = 1400;
    private static final double CANVAS_PADDING_X = 220;
    private static final double CANVAS_PADDING_Y = 180;

    public void layout(MindMap map) {
        if (map == null || map.getRoot() == null) {
            return;
        }
        // 先测量文本尺寸，让布局计算使用节点的实际宽高。
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
            double leftHeight = 0;
            double rightHeight = 0;
            // AUTO 布局将一级分支分配到当前总高度较短的一侧。
            for (MindNode child : root.getChildren()) {
                double childHeight = subtreeHeight(child);
                if (rightHeight <= leftHeight) {
                    right.add(child);
                    rightHeight += childHeight + V_GAP;
                } else {
                    left.add(child);
                    leftHeight += childHeight + V_GAP;
                }
            }
            layoutSide(right, 1, root.getX() + root.getWidth() + H_GAP, ROOT_Y, true);
            layoutSide(left, -1, root.getX() - H_GAP, ROOT_Y, true);
        }
        shiftIntoPositiveArea(map);
    }

    private void measureNode(MindNode node) {
        String text = node.getText() == null ? "" : node.getText();
        double minWidth = node.isRoot() ? 150 : 126;
        double maxWidth = 230;
        Text singleLine = measuredText(node, text);
        double naturalWidth = singleLine.getLayoutBounds().getWidth() + 46;
        double width = Math.min(maxWidth, Math.max(minWidth, naturalWidth));
        Text wrapped = measuredText(node, text);
        wrapped.setWrappingWidth(Math.max(80, width - 34));
        double height = Math.max(node.isRoot() ? 58 : 48, wrapped.getLayoutBounds().getHeight() + 26);
        node.setMeasuredSize(width, height);
    }

    private Text measuredText(MindNode node, String text) {
        Text measured = new Text(text);
        measured.setFont(Font.font(node.getStyle().getFontFamily(),
                node.getStyle().isBold() ? FontWeight.BOLD : FontWeight.NORMAL,
                node.getStyle().isItalic() ? FontPosture.ITALIC : FontPosture.REGULAR,
                node.getStyle().getFontSize()));
        return measured;
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
                // 围绕父节点中心线递归摆放可见子节点。
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
        CanvasBounds bounds = canvasBounds(map);
        if (bounds == null) {
            return MIN_CANVAS;
        }
        double leftExpansion = bounds.minX() < 0 ? -bounds.minX() + CANVAS_PADDING_X : 0;
        return Math.max(MIN_CANVAS, leftExpansion + bounds.maxX() + CANVAS_PADDING_X);
    }

    public double preferredHeight(MindMap map) {
        CanvasBounds bounds = canvasBounds(map);
        if (bounds == null) {
            return MIN_CANVAS * 0.7;
        }
        double topExpansion = bounds.minY() < 0 ? -bounds.minY() + CANVAS_PADDING_Y : 0;
        return Math.max(MIN_CANVAS * 0.7, topExpansion + bounds.maxY() + CANVAS_PADDING_Y);
    }

    private CanvasBounds canvasBounds(MindMap map) {
        if (map == null || map.getRoot() == null || map.visibleNodes().isEmpty()) {
            return null;
        }
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (MindNode node : map.visibleNodes()) {
            minX = Math.min(minX, node.getX());
            minY = Math.min(minY, node.getY());
            maxX = Math.max(maxX, node.getX() + node.getWidth());
            maxY = Math.max(maxY, node.getY() + node.getHeight());
        }
        return new CanvasBounds(minX, minY, maxX, maxY);
    }

    private record CanvasBounds(double minX, double minY, double maxX, double maxY) {
    }
}
