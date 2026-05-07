package com.example.mindmap.view;

import com.example.mindmap.controller.MindMapController;
import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;
import com.example.mindmap.model.NodeStyle;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MindMapCanvas extends ScrollPane {
    private final MindMapController controller;
    private final Pane canvasPane = new Pane();
    private final Group contentGroup = new Group();
    private final Map<String, StackPane> nodeViews = new HashMap<>();
    private final Set<String> searchIds = new HashSet<>();
    private MindMap map;
    private double dragLastX;
    private double dragLastY;
    private boolean dragging;

    public MindMapCanvas(MindMapController controller) {
        this.controller = controller;
        getStyleClass().add("canvas-scroll");
        canvasPane.getStyleClass().add("canvas-pane");
        canvasPane.getChildren().add(contentGroup);
        setContent(canvasPane);
        setPannable(true);
        setFitToWidth(false);
        setFitToHeight(false);
        canvasPane.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getTarget() == canvasPane) {
                controller.clearSelection();
            }
        });
    }

    public void refresh(MindMap map, Set<String> selectedIds, Set<String> searchIds) {
        this.map = map;
        this.searchIds.clear();
        this.searchIds.addAll(searchIds);
        contentGroup.getChildren().clear();
        nodeViews.clear();
        if (map == null || map.getRoot() == null) {
            return;
        }
        contentGroup.setScaleX(map.getZoom());
        contentGroup.setScaleY(map.getZoom());
        canvasPane.setPrefSize(
                controller.preferredCanvasWidth() * map.getZoom(),
                controller.preferredCanvasHeight() * map.getZoom()
        );
        drawConnections();
        for (MindNode node : map.visibleNodes()) {
            drawNode(node, selectedIds.contains(node.getId()), searchIds.contains(node.getId()));
        }
    }

    private void drawConnections() {
        for (MindNode node : map.visibleNodes()) {
            MindNode parent = node.getParent();
            if (parent == null || !map.visibleNodes().contains(parent)) {
                continue;
            }
            CubicCurve curve = new CubicCurve();
            double startX = parent.getCenterX();
            double startY = parent.getCenterY();
            double endX = node.getCenterX();
            double endY = node.getCenterY();
            double controlOffset = Math.max(80, Math.abs(endX - startX) * 0.5);
            curve.setStartX(startX);
            curve.setStartY(startY);
            curve.setControlX1(startX + (endX > startX ? controlOffset : -controlOffset));
            curve.setControlY1(startY);
            curve.setControlX2(endX + (endX > startX ? -controlOffset : controlOffset));
            curve.setControlY2(endY);
            curve.setEndX(endX);
            curve.setEndY(endY);
            curve.getStyleClass().add("connector");
            contentGroup.getChildren().add(curve);
        }
    }

    private void drawNode(MindNode node, boolean selected, boolean searchHit) {
        StackPane box = new StackPane();
        box.getStyleClass().add(node.isRoot() ? "mind-node-root" : "mind-node");
        if (selected) {
            box.getStyleClass().add("mind-node-selected");
        }
        if (searchHit) {
            box.getStyleClass().add("mind-node-search");
        }
        box.setLayoutX(node.getX());
        box.setLayoutY(node.getY());
        box.setPrefSize(node.getWidth(), node.getHeight());
        box.setMinSize(node.getWidth(), node.getHeight());
        box.setMaxSize(node.getWidth(), node.getHeight());
        String borderColor = selected ? "#2563EB" : NodeStyle.toHex(node.getStyle().getBorderColor());
        String borderWidth = selected ? "3" : "1.4";
        box.setStyle("-fx-background-color: " + NodeStyle.toHex(node.getStyle().getFillColor()) + ";"
                + "-fx-border-color: " + borderColor + ";"
                + "-fx-border-width: " + borderWidth + ";"
                + "-fx-text-fill: " + NodeStyle.toHex(node.getStyle().getTextColor()) + ";");

        Text text = new Text(node.getText());
        text.setFill(node.getStyle().getTextColor());
        text.setWrappingWidth(Math.max(76, node.getWidth() - 28));
        text.setTextAlignment(TextAlignment.CENTER);
        box.getChildren().add(text);
        StackPane.setMargin(text, new Insets(8, 14, 8, 14));
        box.setEffect(new DropShadow(14, Color.rgb(15, 23, 42, 0.08)));

        box.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                controller.beginDragSnapshot();
                Point2D point = contentGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
                dragLastX = point.getX();
                dragLastY = point.getY();
                dragging = false;
                if (!controller.isSelected(node)) {
                    controller.selectNode(node, event.isControlDown());
                }
                event.consume();
            }
        });
        box.setOnMouseDragged(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                Point2D point = contentGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
                double dx = point.getX() - dragLastX;
                double dy = point.getY() - dragLastY;
                dragLastX = point.getX();
                dragLastY = point.getY();
                dragging = dragging || Math.abs(dx) + Math.abs(dy) > 0.6;
                controller.dragSelectedBy(dx, dy);
                event.consume();
            }
        });
        box.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (dragging) {
                    controller.finishDragSnapshot();
                } else if (event.getClickCount() == 2) {
                    controller.renameNode(node);
                } else {
                    controller.selectNode(node, event.isControlDown());
                }
                dragging = false;
                event.consume();
            }
        });
        box.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                controller.selectNode(node, false);
                contextMenu(node).show(box, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });

        contentGroup.getChildren().add(box);
        nodeViews.put(node.getId(), box);

        if (!node.getChildren().isEmpty()) {
            drawCollapseHandle(node);
        }
    }

    private void drawCollapseHandle(MindNode node) {
        double x = node.getCenterX() + (node.isRoot() ? node.getWidth() / 2 + 10 : 0);
        if (!node.isRoot() && node.getParent() != null && node.getX() < node.getParent().getX()) {
            x = node.getX() - 12;
        } else if (!node.isRoot()) {
            x = node.getX() + node.getWidth() + 12;
        }
        Circle circle = new Circle(x, node.getCenterY(), 10);
        circle.getStyleClass().add("collapse-handle");
        Label symbol = new Label(node.isCollapsed() ? "+" : "-");
        symbol.getStyleClass().add("collapse-symbol");
        symbol.setLayoutX(x - 4);
        symbol.setLayoutY(node.getCenterY() - 10);
        circle.setOnMouseClicked(event -> {
            controller.toggleCollapse(node);
            event.consume();
        });
        symbol.setOnMouseClicked(event -> {
            controller.toggleCollapse(node);
            event.consume();
        });
        contentGroup.getChildren().addAll(circle, symbol);
    }

    private ContextMenu contextMenu(MindNode node) {
        ContextMenu menu = new ContextMenu();
        MenuItem addChild = new MenuItem("添加子节点");
        addChild.setOnAction(event -> controller.addChildNode());
        MenuItem addSibling = new MenuItem("添加兄弟节点");
        addSibling.setDisable(node.isRoot());
        addSibling.setOnAction(event -> controller.addSiblingNode());
        MenuItem rename = new MenuItem("重命名");
        rename.setOnAction(event -> controller.renameSelectedNode());
        MenuItem delete = new MenuItem("删除");
        delete.setDisable(node.isRoot());
        delete.setOnAction(event -> controller.deleteSelectedNodes());
        MenuItem toggle = new MenuItem(node.isCollapsed() ? "展开" : "折叠");
        toggle.setDisable(node.getChildren().isEmpty());
        toggle.setOnAction(event -> controller.toggleCollapse());
        menu.getItems().addAll(addChild, addSibling, rename, delete, toggle);
        return menu;
    }

    public void scrollToNode(String nodeId) {
        StackPane node = nodeViews.get(nodeId);
        if (node == null || canvasPane.getWidth() == 0 || canvasPane.getHeight() == 0) {
            return;
        }
        Bounds viewport = getViewportBounds();
        double targetX = (node.getLayoutX() * map.getZoom()) - viewport.getWidth() / 2 + node.getWidth() * map.getZoom() / 2;
        double targetY = (node.getLayoutY() * map.getZoom()) - viewport.getHeight() / 2 + node.getHeight() * map.getZoom() / 2;
        setHvalue(clamp(targetX / Math.max(1, canvasPane.getWidth() - viewport.getWidth())));
        setVvalue(clamp(targetY / Math.max(1, canvasPane.getHeight() - viewport.getHeight())));
    }

    public WritableImage snapshotFull() {
        return snapshotFull(1.0);
    }

    public WritableImage snapshotFull(double scale) {
        double oldScaleX = contentGroup.getScaleX();
        double oldScaleY = contentGroup.getScaleY();
        contentGroup.setScaleX(1.0);
        contentGroup.setScaleY(1.0);
        try {
            return snapshotFullAtScale(scale);
        } finally {
            contentGroup.setScaleX(oldScaleX);
            contentGroup.setScaleY(oldScaleY);
        }
    }

    private WritableImage snapshotFullAtScale(double scale) {
        Bounds bounds = contentGroup.getLayoutBounds();
        double pad = 48;
        double safeScale = Math.max(1.0, scale);
        double width = Math.max(1, bounds.getMaxX() - bounds.getMinX() + pad * 2);
        double height = Math.max(1, bounds.getMaxY() - bounds.getMinY() + pad * 2);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        params.setTransform(new Scale(safeScale, safeScale)
                .createConcatenation(Transform.translate(-bounds.getMinX() + pad, -bounds.getMinY() + pad)));
        return contentGroup.snapshot(params, new WritableImage(
                (int) Math.ceil(width * safeScale),
                (int) Math.ceil(height * safeScale)));
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
