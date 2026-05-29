package com.example.mindmap.view;

import com.example.mindmap.controller.MindMapController;
import com.example.mindmap.model.ConnectionShape;
import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

import java.util.Set;

public class MindMapCanvas extends ScrollPane {
    private static final double CANVAS_PADDING_X = 220;
    private static final double CANVAS_PADDING_Y = 180;

    private final MindMapController controller;
    private final StackPane canvasHost = new StackPane();
    private final Pane canvasPane = new Pane();
    private final Group contentGroup = new Group();
    private final ConnectionRenderer connectionRenderer;
    private final NodeRenderer nodeRenderer;
    private ContextMenu activeContextMenu;
    private MindMap map;
    private double lastZoom = 1.0;
    private double lastCanvasWidth;
    private double lastCanvasHeight;
    private double lastCanvasOffsetX;
    private double lastCanvasOffsetY;
    private Point2D selectionStart;
    private Rectangle selectionBox;
    private boolean areaSelectionMoved;

    public MindMapCanvas(MindMapController controller) {
        this.controller = controller;
        this.connectionRenderer = new ConnectionRenderer(contentGroup, controller, this::connectionContextMenu, this::showContextMenu);
        this.nodeRenderer = new NodeRenderer(contentGroup, controller,
                () -> connectionRenderer.updateMovedConnections(map), this::showContextMenu);
        getStyleClass().add("canvas-scroll");
        canvasHost.getStyleClass().add("canvas-host");
        canvasHost.setAlignment(Pos.CENTER);
        canvasPane.getStyleClass().add("canvas-pane");
        canvasPane.getChildren().add(contentGroup);
        canvasHost.getChildren().add(canvasPane);
        setContent(canvasHost);
        setPannable(true);
        setFitToWidth(false);
        setFitToHeight(false);
        setHbarPolicy(ScrollBarPolicy.ALWAYS);
        setVbarPolicy(ScrollBarPolicy.ALWAYS);
        viewportBoundsProperty().addListener((observable, oldValue, newValue) -> updateCanvasHostSize());
        addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                hideActiveContextMenu();
            }
        });
        canvasHost.setOnMouseReleased(event -> {
            if (!event.isControlDown() && event.getButton() == MouseButton.PRIMARY && event.getTarget() == canvasHost) {
                controller.clearSelection();
                event.consume();
            }
        });
        canvasHost.setOnContextMenuRequested(event -> {
            if (event.getTarget() == canvasHost) {
                showContextMenu(canvasContextMenu(), canvasHost, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });
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
                    controller.selectItemsInArea(selectionBox.getX(), selectionBox.getY(),
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
        canvasPane.setOnContextMenuRequested(event -> {
            if (event.getTarget() == canvasPane) {
                showContextMenu(canvasContextMenu(), canvasPane, event.getScreenX(), event.getScreenY());
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

    private void showContextMenu(ContextMenu menu, javafx.scene.Node anchor, double screenX, double screenY) {
        hideActiveContextMenu();
        activeContextMenu = menu;
        menu.setOnHidden(event -> {
            if (activeContextMenu == menu) {
                activeContextMenu = null;
            }
        });
        menu.show(anchor, screenX, screenY);
    }

    private void hideActiveContextMenu() {
        if (activeContextMenu == null) {
            return;
        }
        ContextMenu menu = activeContextMenu;
        activeContextMenu = null;
        menu.hide();
    }

    public void refresh(MindMap map, Set<String> selectedIds, Set<String> selectedConnectionIds,
                        Set<String> searchIds, String searchKeyword) {
        MindMap previousMap = this.map;
        double oldZoom = lastZoom;
        Point2D oldCenter = previousMap == map ? getViewportLogicalCenter(oldZoom) : null;
        this.map = map;
        // 根据模型状态重新绘制画布，使选中、搜索命中和缩放状态保持同步。
        contentGroup.getChildren().clear();
        nodeRenderer.clear();
        connectionRenderer.clear();
        if (map == null || map.getRoot() == null) {
            return;
        }
        CanvasMetrics metrics = measureCanvas(map);
        boolean viewportAnchorChanged = oldCenter != null && (
                Math.abs(map.getZoom() - oldZoom) > 0.0001
                        || Math.abs(metrics.width() - lastCanvasWidth) > 0.0001
                        || Math.abs(metrics.height() - lastCanvasHeight) > 0.0001
                        || Math.abs(metrics.offsetX() - lastCanvasOffsetX) > 0.0001
                        || Math.abs(metrics.offsetY() - lastCanvasOffsetY) > 0.0001
        );
        contentGroup.getTransforms().setAll(new Scale(map.getZoom(), map.getZoom(), 0, 0));
        contentGroup.setLayoutX(metrics.offsetX() * map.getZoom());
        contentGroup.setLayoutY(metrics.offsetY() * map.getZoom());
        canvasPane.setStyle(CanvasBackground.styleFor(map));
        canvasPane.setPrefSize(
                metrics.width() * map.getZoom(),
                metrics.height() * map.getZoom()
        );
        updateCanvasHostSize();
        connectionRenderer.setSelectedConnectionIds(selectedConnectionIds);
        connectionRenderer.draw(map);
        nodeRenderer.draw(map, selectedIds, searchIds, searchKeyword);
        if (viewportAnchorChanged) {
            Platform.runLater(() -> centerViewportOn(oldCenter));
        }
        lastZoom = map.getZoom();
        lastCanvasWidth = metrics.width();
        lastCanvasHeight = metrics.height();
        lastCanvasOffsetX = metrics.offsetX();
        lastCanvasOffsetY = metrics.offsetY();
    }

    private ContextMenu canvasContextMenu() {
        ContextMenu menu = new ContextMenu();
        Menu layoutMenu = new Menu("布局");
        layoutMenu.getItems().addAll(
                menuItem("自动布局", () -> controller.changeLayout(com.example.mindmap.model.LayoutType.AUTO)),
                menuItem("左侧布局", () -> controller.changeLayout(com.example.mindmap.model.LayoutType.LEFT)),
                menuItem("右侧布局", () -> controller.changeLayout(com.example.mindmap.model.LayoutType.RIGHT))
        );
        Menu viewMenu = new Menu("视图");
        viewMenu.getItems().addAll(
                menuItem("放大", controller::zoomIn),
                menuItem("缩小", controller::zoomOut),
                menuItem("100%", controller::resetZoom),
                menuItem("适应窗口", controller::fitToWindow)
        );
        Menu connectionMenu = new Menu("连接线");
        connectionMenu.getItems().addAll(
                menuItem(map != null && map.isConnectionDashed() ? "切换为实线" : "切换为虚线", controller::toggleConnectionDashed),
                new SeparatorMenuItem(),
                menuItem("细线", () -> controller.changeConnectionWidth(1.5)),
                menuItem("标准线", () -> controller.changeConnectionWidth(2.2)),
                menuItem("粗线", () -> controller.changeConnectionWidth(4.0)),
                new SeparatorMenuItem(),
                menuItem("曲线", () -> controller.changeConnectionShape(ConnectionShape.CURVE)),
                menuItem("折线", () -> controller.changeConnectionShape(ConnectionShape.ELBOW))
        );
        Menu canvasMenu = new Menu("画布背景");
        canvasMenu.getItems().addAll(
                menuItem("白色", () -> controller.changeCanvasColor(Color.WHITE)),
                menuItem("浅灰", () -> controller.changeCanvasColor(Color.web("#F8FAFC"))),
                menuItem("浅黄", () -> controller.changeCanvasColor(Color.web("#FFF7ED")))
        );
        menu.getItems().addAll(
                menuItem("查找替换", controller::showFindReplaceDialog),
                new SeparatorMenuItem(),
                menuItem("全部展开", controller::expandAll),
                menuItem("全部收起", controller::collapseAll),
                new SeparatorMenuItem(),
                layoutMenu,
                viewMenu,
                connectionMenu,
                canvasMenu
        );
        return menu;
    }

    private ContextMenu connectionContextMenu(MindNode child) {
        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(
                menuItem("曲线", () -> controller.changeConnectionShape(ConnectionShape.CURVE)),
                menuItem("折线", () -> controller.changeConnectionShape(ConnectionShape.ELBOW)),
                new SeparatorMenuItem(),
                menuItem(child.getConnectionStyle().isDashed() ? "切换为实线" : "切换为虚线", controller::toggleConnectionDashed),
                menuItem("细线", () -> controller.changeConnectionWidth(1.5)),
                menuItem("标准线", () -> controller.changeConnectionWidth(2.2)),
                menuItem("粗线", () -> controller.changeConnectionWidth(4.0))
        );
        return menu;
    }

    private MenuItem menuItem(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> action.run());
        return item;
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
        StackPane node = nodeRenderer.getNodeView(nodeId);
        if (map == null || node == null || canvasHost.getWidth() == 0 || canvasHost.getHeight() == 0) {
            return;
        }
        Bounds viewport = getViewportBounds();
        double targetX = canvasPane.getLayoutX() + contentGroup.getLayoutX() + (node.getLayoutX() * map.getZoom())
                - viewport.getWidth() / 2 + node.getWidth() * map.getZoom() / 2;
        double targetY = canvasPane.getLayoutY() + contentGroup.getLayoutY() + (node.getLayoutY() * map.getZoom())
                - viewport.getHeight() / 2 + node.getHeight() * map.getZoom() / 2;
        setHvalue(clamp(targetX / Math.max(1, canvasHost.getWidth() - viewport.getWidth())));
        setVvalue(clamp(targetY / Math.max(1, canvasHost.getHeight() - viewport.getHeight())));
    }

    private CanvasMetrics measureCanvas(MindMap map) {
        double baseWidth = controller.preferredCanvasWidth();
        double baseHeight = controller.preferredCanvasHeight();
        if (map == null || map.getRoot() == null || map.visibleNodes().isEmpty()) {
            return new CanvasMetrics(baseWidth, baseHeight, 0, 0);
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
        double offsetX = minX < 0 ? -minX + CANVAS_PADDING_X : 0;
        double offsetY = minY < 0 ? -minY + CANVAS_PADDING_Y : 0;
        double width = Math.max(baseWidth, offsetX + maxX + CANVAS_PADDING_X);
        double height = Math.max(baseHeight, offsetY + maxY + CANVAS_PADDING_Y);
        return new CanvasMetrics(width, height, offsetX, offsetY);
    }

    private void updateCanvasHostSize() {
        Bounds viewport = getViewportBounds();
        double width = Math.max(canvasPane.getPrefWidth(), viewport.getWidth());
        double height = Math.max(canvasPane.getPrefHeight(), viewport.getHeight());
        canvasHost.setMinSize(width, height);
        canvasHost.setPrefSize(width, height);
    }

    private Point2D getViewportLogicalCenter(double zoom) {
        Bounds viewport = getViewportBounds();
        if (map == null || viewport.getWidth() <= 0 || viewport.getHeight() <= 0) {
            return null;
        }
        double scrollX = getHvalue() * Math.max(1, canvasHost.getWidth() - viewport.getWidth());
        double scrollY = getVvalue() * Math.max(1, canvasHost.getHeight() - viewport.getHeight());
        double safeZoom = Math.max(0.0001, zoom);
        double logicalX = (scrollX + viewport.getWidth() / 2 - canvasPane.getLayoutX() - contentGroup.getLayoutX()) / safeZoom;
        double logicalY = (scrollY + viewport.getHeight() / 2 - canvasPane.getLayoutY() - contentGroup.getLayoutY()) / safeZoom;
        return new Point2D(logicalX, logicalY);
    }

    private void centerViewportOn(Point2D logicalCenter) {
        if (map == null) {
            return;
        }
        updateCanvasHostSize();
        canvasHost.applyCss();
        canvasHost.layout();
        Bounds viewport = getViewportBounds();
        double targetX = canvasPane.getLayoutX() + contentGroup.getLayoutX() + logicalCenter.getX() * map.getZoom() - viewport.getWidth() / 2;
        double targetY = canvasPane.getLayoutY() + contentGroup.getLayoutY() + logicalCenter.getY() * map.getZoom() - viewport.getHeight() / 2;
        setHvalue(clamp(targetX / Math.max(1, canvasHost.getWidth() - viewport.getWidth())));
        setVvalue(clamp(targetY / Math.max(1, canvasHost.getHeight() - viewport.getHeight())));
    }

    private record CanvasMetrics(double width, double height, double offsetX, double offsetY) {
    }

    public WritableImage snapshotFull() {
        return snapshotFull(1.0);
    }

    public WritableImage snapshotFull(double scale) {
        return CanvasSnapshotter.snapshotFull(contentGroup, map, scale);
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
