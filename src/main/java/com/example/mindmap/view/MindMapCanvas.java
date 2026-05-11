package com.example.mindmap.view;

import com.example.mindmap.controller.MindMapController;
import com.example.mindmap.model.ConnectionShape;
import com.example.mindmap.model.ConnectionStyle;
import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;
import com.example.mindmap.model.NodeStyle;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SeparatorMenuItem;
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
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
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
    private static final double CANVAS_PADDING_X = 220;
    private static final double CANVAS_PADDING_Y = 180;

    private final MindMapController controller;
    private final StackPane canvasHost = new StackPane();
    private final Pane canvasPane = new Pane();
    private final Group contentGroup = new Group();
    private final Map<String, StackPane> nodeViews = new HashMap<>();
    private final Map<String, ConnectionView> connectionViews = new HashMap<>();
    private final Set<String> searchIds = new HashSet<>();
    private final Set<String> selectedConnectionIds = new HashSet<>();
    private MindMap map;
    private double lastZoom = 1.0;
    private double lastCanvasWidth;
    private double lastCanvasHeight;
    private double lastCanvasOffsetX;
    private double lastCanvasOffsetY;
    private double dragLastX;
    private double dragLastY;
    private Point2D selectionStart;
    private Rectangle selectionBox;
    private boolean areaSelectionMoved;
    private boolean dragging;

    private enum ResizeHandle {
        TOP_LEFT, TOP, TOP_RIGHT, RIGHT, BOTTOM_RIGHT, BOTTOM, BOTTOM_LEFT, LEFT
    }

    private record ConnectionView(Shape hoverGlow, Shape selectionGlow, Shape visual, Shape hitArea,
                                  Circle startHandle, Circle middleHandle, Circle endHandle,
                                  MindNode parent, MindNode child) {
    }

    public MindMapCanvas(MindMapController controller) {
        this.controller = controller;
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
        canvasHost.setOnMouseReleased(event -> {
            if (!event.isControlDown() && event.getButton() == MouseButton.PRIMARY && event.getTarget() == canvasHost) {
                controller.clearSelection();
                event.consume();
            }
        });
        canvasHost.setOnContextMenuRequested(event -> {
            if (event.getTarget() == canvasHost) {
                canvasContextMenu().show(canvasHost, event.getScreenX(), event.getScreenY());
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
                canvasContextMenu().show(canvasPane, event.getScreenX(), event.getScreenY());
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

    public void refresh(MindMap map, Set<String> selectedIds, Set<String> selectedConnectionIds, Set<String> searchIds) {
        MindMap previousMap = this.map;
        double oldZoom = lastZoom;
        Point2D oldCenter = previousMap == map ? getViewportLogicalCenter(oldZoom) : null;
        this.map = map;
        this.searchIds.clear();
        this.searchIds.addAll(searchIds);
        this.selectedConnectionIds.clear();
        this.selectedConnectionIds.addAll(selectedConnectionIds);
        contentGroup.getChildren().clear();
        nodeViews.clear();
        connectionViews.clear();
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
        canvasPane.setStyle(canvasBackgroundStyle(map));
        canvasPane.setPrefSize(
                metrics.width() * map.getZoom(),
                metrics.height() * map.getZoom()
        );
        updateCanvasHostSize();
        drawConnections();
        for (MindNode node : map.visibleNodes()) {
            drawNode(node, selectedIds.contains(node.getId()), searchIds.contains(node.getId()));
        }
        if (viewportAnchorChanged) {
            Platform.runLater(() -> centerViewportOn(oldCenter));
        }
        lastZoom = map.getZoom();
        lastCanvasWidth = metrics.width();
        lastCanvasHeight = metrics.height();
        lastCanvasOffsetX = metrics.offsetX();
        lastCanvasOffsetY = metrics.offsetY();
    }

    private String canvasBackgroundStyle(MindMap map) {
        String color = NodeStyle.toHex(map.getCanvasColor());
        String imageUri = map.getCanvasImageUri();
        if (imageUri == null || imageUri.isBlank()) {
            return "-fx-background-color: " + color + ";";
        }
        return "-fx-background-color: " + color + ";"
                + "-fx-background-image: url(\"" + cssUrl(imageUri) + "\");"
                + "-fx-background-repeat: no-repeat;"
                + "-fx-background-position: center center;"
                + "-fx-background-size: cover;";
    }

    private String cssUrl(String uri) {
        return uri.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void drawConnections() {
        for (MindNode node : map.visibleNodes()) {
            MindNode parent = node.getParent();
            if (parent == null || !map.visibleNodes().contains(parent)) {
                continue;
            }
            boolean selected = selectedConnectionIds.contains(node.getId());
            Shape hoverGlow = createConnectionShape(node.getConnectionStyle().getShape());
            Shape selectionGlow = createConnectionShape(node.getConnectionStyle().getShape());
            Shape visual = createConnectionShape(node.getConnectionStyle().getShape());
            Shape hitArea = createConnectionShape(node.getConnectionStyle().getShape());
            Circle startHandle = createConnectionHandle();
            Circle middleHandle = createConnectionHandle();
            Circle endHandle = createConnectionHandle();
            styleConnectionGlow(hoverGlow, false, node.getConnectionStyle());
            styleConnectionGlow(selectionGlow, true, node.getConnectionStyle());
            styleConnection(visual, node.getConnectionStyle());
            styleConnectionHitArea(hitArea);
            ConnectionView view = new ConnectionView(hoverGlow, selectionGlow, visual, hitArea,
                    startHandle, middleHandle, endHandle, parent, node);
            updateConnectionView(view);
            setConnectionSelected(view, selected);
            installConnectionHandlers(visual, node);
            installConnectionHandlers(hitArea, node);
            contentGroup.getChildren().addAll(selectionGlow, hoverGlow, visual,
                    startHandle, middleHandle, endHandle, hitArea);
            connectionViews.put(connectionKey(parent, node), view);
        }
    }

    private Shape createConnectionShape(ConnectionShape shape) {
        return shape == ConnectionShape.ELBOW ? new Path() : new CubicCurve();
    }

    private void styleConnection(Shape shape, ConnectionStyle style) {
        shape.getStyleClass().add("connector");
        Color strokeColor = style.getColor();
        double strokeWidth = style.getWidth();
        shape.setStyle("-fx-stroke: " + NodeStyle.toHex(strokeColor) + ";"
                + "-fx-stroke-width: " + strokeWidth + ";"
                + "-fx-fill: transparent;");
        shape.setFill(Color.TRANSPARENT);
        shape.setStroke(strokeColor);
        shape.setStrokeWidth(strokeWidth);
        shape.setStrokeLineCap(StrokeLineCap.ROUND);
        shape.getStrokeDashArray().clear();
        if (style.isDashed()) {
            shape.getStrokeDashArray().addAll(12.0, 8.0);
        }
    }

    private void styleConnectionGlow(Shape shape, boolean selected, ConnectionStyle style) {
        shape.setMouseTransparent(true);
        shape.setFill(Color.TRANSPARENT);
        shape.setStroke(selected ? Color.rgb(37, 99, 235, 0.30) : Color.rgb(96, 165, 250, 0.24));
        shape.setStrokeWidth(selected ? Math.max(style.getWidth() + 8.0, 10.0) : Math.max(style.getWidth() + 5.0, 7.0));
        shape.setStrokeLineCap(StrokeLineCap.ROUND);
        shape.setVisible(false);
    }

    private Circle createConnectionHandle() {
        Circle circle = new Circle(4.5);
        circle.setMouseTransparent(true);
        circle.setFill(Color.WHITE);
        circle.setStroke(Color.web("#2563EB"));
        circle.setStrokeWidth(1.8);
        circle.setVisible(false);
        return circle;
    }

    private void styleConnectionHitArea(Shape shape) {
        shape.setFill(Color.TRANSPARENT);
        shape.setStroke(Color.TRANSPARENT);
        shape.setStrokeWidth(14);
        shape.setStrokeLineCap(StrokeLineCap.ROUND);
        shape.setCursor(Cursor.HAND);
    }

    private void installConnectionHandlers(Shape shape, MindNode child) {
        shape.setOnMouseEntered(event -> setConnectionHover(child, true));
        shape.setOnMouseExited(event -> setConnectionHover(child, false));
        shape.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                controller.selectConnection(child.getId(), event.isControlDown());
                event.consume();
            }
        });
        shape.setOnContextMenuRequested(event -> {
            controller.prepareConnectionForContext(child.getId());
            connectionContextMenu(child).show(shape, event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }

    private void setConnectionHover(MindNode child, boolean hovered) {
        MindNode parent = child.getParent();
        if (parent == null) {
            return;
        }
        ConnectionView view = connectionViews.get(connectionKey(parent, child));
        if (view != null && !selectedConnectionIds.contains(child.getId())) {
            view.hoverGlow().setVisible(hovered);
        }
    }

    private void setConnectionSelected(ConnectionView view, boolean selected) {
        view.selectionGlow().setVisible(selected);
        view.hoverGlow().setVisible(false);
        view.startHandle().setVisible(selected);
        view.middleHandle().setVisible(selected);
        view.endHandle().setVisible(selected);
    }

    private void updateConnectionView(ConnectionView view) {
        updateConnectionShape(view.selectionGlow(), view.parent(), view.child());
        updateConnectionShape(view.hoverGlow(), view.parent(), view.child());
        updateConnectionShape(view.visual(), view.parent(), view.child());
        updateConnectionShape(view.hitArea(), view.parent(), view.child());
        updateConnectionHandles(view);
    }

    private void updateConnectionShape(Shape shape, MindNode parent, MindNode node) {
        if (shape instanceof CubicCurve curve) {
            updateCurve(curve, parent, node);
        } else if (shape instanceof Path path) {
            updateElbow(path, parent, node);
        }
    }

    private void updateCurve(CubicCurve curve, MindNode parent, MindNode node) {
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
    }

    private void updateElbow(Path path, MindNode parent, MindNode node) {
        double startX = parent.getCenterX();
        double startY = parent.getCenterY();
        double endX = node.getCenterX();
        double endY = node.getCenterY();
        double midX = (startX + endX) / 2.0;
        path.getElements().setAll(
                new MoveTo(startX, startY),
                new LineTo(midX, startY),
                new LineTo(midX, endY),
                new LineTo(endX, endY)
        );
    }

    private void updateConnectionHandles(ConnectionView view) {
        Point2D start = connectionStart(view.parent());
        Point2D middle = connectionMiddle(view.parent(), view.child(), view.child().getConnectionStyle().getShape());
        Point2D end = connectionEnd(view.child());
        placeConnectionHandle(view.startHandle(), start);
        placeConnectionHandle(view.middleHandle(), middle);
        placeConnectionHandle(view.endHandle(), end);
    }

    private Point2D connectionStart(MindNode parent) {
        return new Point2D(parent.getCenterX(), parent.getCenterY());
    }

    private Point2D connectionEnd(MindNode child) {
        return new Point2D(child.getCenterX(), child.getCenterY());
    }

    private Point2D connectionMiddle(MindNode parent, MindNode child, ConnectionShape shape) {
        double startX = parent.getCenterX();
        double startY = parent.getCenterY();
        double endX = child.getCenterX();
        double endY = child.getCenterY();
        if (shape == ConnectionShape.ELBOW) {
            return new Point2D((startX + endX) / 2.0, (startY + endY) / 2.0);
        }
        double controlOffset = Math.max(80, Math.abs(endX - startX) * 0.5);
        double controlX1 = startX + (endX > startX ? controlOffset : -controlOffset);
        double controlY1 = startY;
        double controlX2 = endX + (endX > startX ? -controlOffset : controlOffset);
        double controlY2 = endY;
        double t = 0.5;
        double x = Math.pow(1 - t, 3) * startX
                + 3 * Math.pow(1 - t, 2) * t * controlX1
                + 3 * (1 - t) * Math.pow(t, 2) * controlX2
                + Math.pow(t, 3) * endX;
        double y = Math.pow(1 - t, 3) * startY
                + 3 * Math.pow(1 - t, 2) * t * controlY1
                + 3 * (1 - t) * Math.pow(t, 2) * controlY2
                + Math.pow(t, 3) * endY;
        return new Point2D(x, y);
    }

    private void placeConnectionHandle(Circle handle, Point2D point) {
        handle.setCenterX(point.getX());
        handle.setCenterY(point.getY());
    }

    private String connectionKey(MindNode parent, MindNode node) {
        return parent.getId() + "->" + node.getId();
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
                updateMovedConnections();
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
                controller.prepareNodeForDrag(node, false);
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
                updateMovedConnections();
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

    private void updateMovedConnections() {
        if (map == null) {
            return;
        }
        for (MindNode node : map.visibleNodes()) {
            MindNode parent = node.getParent();
            if (parent == null) {
                continue;
            }
            ConnectionView view = connectionViews.get(connectionKey(parent, node));
            if (view != null) {
                updateConnectionView(view);
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
        MenuItem rename = menuItem("编辑文本", controller::renameSelectedNode);
        MenuItem addChild = menuItem("添加子节点", controller::addChildNode);
        MenuItem addSibling = menuItem("添加兄弟节点", controller::addSiblingNode);
        addSibling.setDisable(node.isRoot());
        MenuItem toggle = menuItem(node.isCollapsed() ? "展开分支" : "折叠分支", controller::toggleCollapse);
        toggle.setDisable(node.getChildren().isEmpty());
        Menu styleMenu = new Menu("节点样式");
        styleMenu.getItems().addAll(
                menuItem("白底深字", () -> controller.applyNodePreset(Color.WHITE, Color.web("#0F172A"))),
                menuItem("蓝底白字", () -> controller.applyNodePreset(Color.web("#2563EB"), Color.WHITE)),
                menuItem("浅黄底", () -> controller.changeFillColor(Color.web("#FEF3C7"))),
                new SeparatorMenuItem(),
                menuItem("左对齐", () -> controller.changeTextAlignment(TextAlignment.LEFT)),
                menuItem("居中对齐", () -> controller.changeTextAlignment(TextAlignment.CENTER)),
                menuItem("右对齐", () -> controller.changeTextAlignment(TextAlignment.RIGHT))
        );
        MenuItem delete = menuItem("删除节点", controller::deleteSelectedNodes);
        delete.setDisable(node.isRoot());
        menu.getItems().addAll(rename, new SeparatorMenuItem(), addChild, addSibling, toggle,
                new SeparatorMenuItem(), styleMenu, new SeparatorMenuItem(), delete);
        return menu;
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
        StackPane node = nodeViews.get(nodeId);
        if (node == null || canvasHost.getWidth() == 0 || canvasHost.getHeight() == 0) {
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
