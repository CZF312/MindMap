package com.example.mindmap.view;

import com.example.mindmap.controller.MindMapController;
import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;
import com.example.mindmap.model.NodeStyle;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private Point2D selectionStart;
    private Rectangle selectionBox;
    private boolean areaSelectionMoved;
    private boolean dragging;

    private enum ResizeHandle {
        TOP_LEFT, TOP, TOP_RIGHT, RIGHT, BOTTOM_RIGHT, BOTTOM, BOTTOM_LEFT, LEFT
    }

    public MindMapCanvas(MindMapController controller) {
        this.controller = controller;
        getStyleClass().add("canvas-scroll");
        canvasPane.getStyleClass().add("canvas-pane");
        canvasPane.getChildren().add(contentGroup);
        setContent(canvasPane);
        setPannable(true);
        setFitToWidth(false);
        setFitToHeight(false);
        canvasPane.setOnMousePressed(event -> {
            if (event.isControlDown() && event.getButton() == MouseButton.PRIMARY && event.getTarget() == canvasPane) {
                selectionStart = contentGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
                areaSelectionMoved = false;
                selectionBox = new Rectangle(selectionStart.getX(), selectionStart.getY(), 0, 0);
                selectionBox.getStyleClass().add("selection-box");
                selectionBox.setMouseTransparent(true);
                contentGroup.getChildren().add(selectionBox);
                event.consume();
            }
        });
        canvasPane.setOnMouseDragged(event -> {
            if (selectionStart != null && event.getButton() == MouseButton.PRIMARY) {
                Point2D current = contentGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
                areaSelectionMoved = areaSelectionMoved
                        || Math.abs(current.getX() - selectionStart.getX()) + Math.abs(current.getY() - selectionStart.getY()) > 5;
                updateSelectionBox(current);
                event.consume();
            }
        });
        canvasPane.setOnMouseReleased(event -> {
            if (selectionStart != null && event.getButton() == MouseButton.PRIMARY) {
                if (selectionBox != null) {
                    contentGroup.getChildren().remove(selectionBox);
                }
                if (areaSelectionMoved && selectionBox != null) {
                    controller.selectNodesInArea(selectionBox.getX(), selectionBox.getY(),
                            selectionBox.getX() + selectionBox.getWidth(),
                            selectionBox.getY() + selectionBox.getHeight());
                } else {
                    controller.clearSelection();
                }
                selectionStart = null;
                selectionBox = null;
                areaSelectionMoved = false;
                event.consume();
            } else if (!event.isControlDown() && event.getButton() == MouseButton.PRIMARY && event.getTarget() == canvasPane) {
                controller.clearSelection();
                event.consume();
            }
        });
        addEventFilter(ScrollEvent.SCROLL, event -> {
            if (!event.isControlDown() || event.getDeltaY() == 0) {
                return;
            }
            if (event.getDeltaY() > 0) {
                controller.zoomIn();
            } else {
                controller.zoomOut();
            }
            event.consume();
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
        contentGroup.getTransforms().setAll(new Scale(map.getZoom(), map.getZoom(), 0, 0));
        canvasPane.setStyle("-fx-background-color: " + NodeStyle.toHex(map.getCanvasColor()) + ";");
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
        text.setTextAlignment(node.getStyle().getAlignment());
        text.setUnderline(node.getStyle().isUnderline());
        text.setStrikethrough(node.getStyle().isStrikethrough());
        text.setFont(Font.font(node.getStyle().getFontFamily(),
                node.getStyle().isBold() ? FontWeight.BOLD : FontWeight.NORMAL,
                node.getStyle().isItalic() ? FontPosture.ITALIC : FontPosture.REGULAR,
                node.getStyle().getFontSize()));
        box.getChildren().add(text);
        StackPane.setMargin(text, new Insets(8, 14, 8, 14));
        StackPane.setAlignment(text, alignment(node.getStyle().getAlignment()));
        box.setEffect(new DropShadow(14, Color.rgb(15, 23, 42, 0.08)));

        box.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                controller.beginDragSnapshot();
                Point2D point = contentGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
                dragLastX = point.getX();
                dragLastY = point.getY();
                dragging = false;
                if (!controller.isSelected(node)) {
                    controller.prepareNodeForDrag(node, event.isControlDown());
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
                updateMovedNodeViews();
                event.consume();
            }
        });
        box.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (dragging) {
                    controller.finishDragSnapshot();
                } else if (event.getClickCount() == 2) {
                    startInlineEdit(node, box);
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

        if (selected) {
            drawResizeHandles(node, box);
        }

        if (!node.getChildren().isEmpty()) {
            drawCollapseHandle(node);
        }
    }

    private Pos alignment(TextAlignment textAlignment) {
        if (textAlignment == TextAlignment.LEFT) {
            return Pos.CENTER_LEFT;
        }
        if (textAlignment == TextAlignment.RIGHT) {
            return Pos.CENTER_RIGHT;
        }
        return Pos.CENTER;
    }

    private void startInlineEdit(MindNode node, StackPane box) {
        TextField editor = new TextField(node.getText());
        editor.getStyleClass().add("node-inline-editor");
        editor.setPrefSize(node.getWidth() - 12, node.getHeight() - 10);
        box.getChildren().setAll(editor);
        editor.requestFocus();
        editor.selectAll();
        boolean[] committed = {false};
        Runnable commit = () -> {
            if (committed[0]) {
                return;
            }
            committed[0] = true;
            controller.renameNodeText(node.getId(), editor.getText());
        };
        editor.setOnAction(event -> commit.run());
        editor.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                commit.run();
            }
        });
    }

    private void drawResizeHandles(MindNode node, StackPane box) {
        for (ResizeHandle handle : ResizeHandle.values()) {
            Rectangle marker = new Rectangle(8, 8);
            marker.getStyleClass().add("resize-handle");
            marker.setCursor(cursor(handle));
            placeHandle(marker, node, handle);
            final double[] startX = new double[1];
            final double[] startY = new double[1];
            final double[] nodeX = new double[1];
            final double[] nodeY = new double[1];
            final double[] nodeWidth = new double[1];
            final double[] nodeHeight = new double[1];
            marker.setOnMousePressed(event -> {
                controller.beginDragSnapshot();
                Point2D point = contentGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
                startX[0] = point.getX();
                startY[0] = point.getY();
                nodeX[0] = node.getX();
                nodeY[0] = node.getY();
                nodeWidth[0] = node.getWidth();
                nodeHeight[0] = node.getHeight();
                event.consume();
            });
            marker.setOnMouseDragged(event -> {
                Point2D point = contentGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
                double dx = point.getX() - startX[0];
                double dy = point.getY() - startY[0];
                resizeNode(node, handle, nodeX[0], nodeY[0], nodeWidth[0], nodeHeight[0], dx, dy);
                box.setLayoutX(node.getX());
                box.setLayoutY(node.getY());
                box.setPrefSize(node.getWidth(), node.getHeight());
                box.setMinSize(node.getWidth(), node.getHeight());
                box.setMaxSize(node.getWidth(), node.getHeight());
                for (javafx.scene.Node child : contentGroup.getChildren()) {
                    if (child instanceof Rectangle rectangle && rectangle.getProperties().get("resizeHandle") instanceof ResizeHandle h
                            && node.getId().equals(rectangle.getProperties().get("nodeId"))) {
                        placeHandle(rectangle, node, h);
                    }
                }
                event.consume();
            });
            marker.setOnMouseReleased(event -> {
                controller.finishDragSnapshot();
                event.consume();
            });
            marker.getProperties().put("resizeHandle", handle);
            marker.getProperties().put("nodeId", node.getId());
            contentGroup.getChildren().add(marker);
        }
    }

    private void resizeNode(MindNode node, ResizeHandle handle, double x, double y, double width, double height, double dx, double dy) {
        double newX = x;
        double newY = y;
        double newWidth = width;
        double newHeight = height;
        if (handle == ResizeHandle.LEFT || handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.BOTTOM_LEFT) {
            newX = x + dx;
            newWidth = width - dx;
        }
        if (handle == ResizeHandle.RIGHT || handle == ResizeHandle.TOP_RIGHT || handle == ResizeHandle.BOTTOM_RIGHT) {
            newWidth = width + dx;
        }
        if (handle == ResizeHandle.TOP || handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.TOP_RIGHT) {
            newY = y + dy;
            newHeight = height - dy;
        }
        if (handle == ResizeHandle.BOTTOM || handle == ResizeHandle.BOTTOM_LEFT || handle == ResizeHandle.BOTTOM_RIGHT) {
            newHeight = height + dy;
        }
        if (newWidth < 60) {
            newX = handle == ResizeHandle.LEFT || handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.BOTTOM_LEFT
                    ? x + width - 60 : newX;
            newWidth = 60;
        }
        if (newHeight < 36) {
            newY = handle == ResizeHandle.TOP || handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.TOP_RIGHT
                    ? y + height - 36 : newY;
            newHeight = 36;
        }
        controller.resizeNode(node.getId(), newX, newY, newWidth, newHeight);
    }

    private void updateMovedNodeViews() {
        if (map == null) {
            return;
        }
        for (MindNode visibleNode : map.visibleNodes()) {
            StackPane view = nodeViews.get(visibleNode.getId());
            if (view != null) {
                view.setLayoutX(visibleNode.getX());
                view.setLayoutY(visibleNode.getY());
            }
        }
        for (javafx.scene.Node child : contentGroup.getChildren()) {
            if (child instanceof Rectangle rectangle
                    && rectangle.getProperties().get("resizeHandle") instanceof ResizeHandle handle
                    && rectangle.getProperties().get("nodeId") instanceof String nodeId) {
                map.findNodeById(nodeId).ifPresent(node -> placeHandle(rectangle, node, handle));
            }
        }
    }

    private void placeHandle(Rectangle marker, MindNode node, ResizeHandle handle) {
        double x = node.getX();
        double y = node.getY();
        double w = node.getWidth();
        double h = node.getHeight();
        double hx = switch (handle) {
            case TOP_LEFT, LEFT, BOTTOM_LEFT -> x - 4;
            case TOP, BOTTOM -> x + w / 2 - 4;
            case TOP_RIGHT, RIGHT, BOTTOM_RIGHT -> x + w - 4;
        };
        double hy = switch (handle) {
            case TOP_LEFT, TOP, TOP_RIGHT -> y - 4;
            case LEFT, RIGHT -> y + h / 2 - 4;
            case BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT -> y + h - 4;
        };
        marker.setX(hx);
        marker.setY(hy);
    }

    private Cursor cursor(ResizeHandle handle) {
        return switch (handle) {
            case TOP_LEFT, BOTTOM_RIGHT -> Cursor.NW_RESIZE;
            case TOP_RIGHT, BOTTOM_LEFT -> Cursor.NE_RESIZE;
            case LEFT, RIGHT -> Cursor.E_RESIZE;
            case TOP, BOTTOM -> Cursor.N_RESIZE;
        };
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

    private void updateSelectionBox(Point2D current) {
        if (selectionBox == null || selectionStart == null) {
            return;
        }
        double minX = Math.min(selectionStart.getX(), current.getX());
        double minY = Math.min(selectionStart.getY(), current.getY());
        double width = Math.abs(current.getX() - selectionStart.getX());
        double height = Math.abs(current.getY() - selectionStart.getY());
        selectionBox.setX(minX);
        selectionBox.setY(minY);
        selectionBox.setWidth(width);
        selectionBox.setHeight(height);
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
        List<Transform> oldTransforms = new ArrayList<>(contentGroup.getTransforms());
        contentGroup.getTransforms().clear();
        try {
            return snapshotFullAtScale(scale);
        } finally {
            contentGroup.getTransforms().setAll(oldTransforms);
        }
    }

    private WritableImage snapshotFullAtScale(double scale) {
        Bounds bounds = contentGroup.getLayoutBounds();
        double pad = 48;
        double safeScale = Math.max(1.0, scale);
        double width = Math.max(1, bounds.getMaxX() - bounds.getMinX() + pad * 2);
        double height = Math.max(1, bounds.getMaxY() - bounds.getMinY() + pad * 2);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(map == null ? Color.WHITE : map.getCanvasColor());
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
