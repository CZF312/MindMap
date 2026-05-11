package com.example.mindmap.view;

import com.example.mindmap.controller.MindMapController;
import com.example.mindmap.model.ConnectionShape;
import com.example.mindmap.model.ConnectionStyle;
import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;
import com.example.mindmap.model.NodeStyle;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

final class ConnectionRenderer {
    private final Group contentGroup;
    private final MindMapController controller;
    private final Function<MindNode, ContextMenu> contextMenuFactory;
    private final Map<String, ConnectionView> connectionViews = new HashMap<>();
    private final Set<String> selectedConnectionIds = new HashSet<>();

    private record ConnectionView(Shape hoverGlow, Shape selectionGlow, Shape visual, Shape hitArea,
                                  Circle startHandle, Circle middleHandle, Circle endHandle,
                                  MindNode parent, MindNode child) {
    }

    ConnectionRenderer(Group contentGroup, MindMapController controller,
                       Function<MindNode, ContextMenu> contextMenuFactory) {
        this.contentGroup = contentGroup;
        this.controller = controller;
        this.contextMenuFactory = contextMenuFactory;
    }

    void setSelectedConnectionIds(Set<String> ids) {
        selectedConnectionIds.clear();
        selectedConnectionIds.addAll(ids);
    }

    void clear() {
        connectionViews.clear();
    }

    void draw(MindMap map) {
        clear();
        List<MindNode> visibleNodes = map.visibleNodes();
        Set<MindNode> visibleNodeSet = new HashSet<>(visibleNodes);
        for (MindNode node : visibleNodes) {
            MindNode parent = node.getParent();
            if (parent == null || !visibleNodeSet.contains(parent)) {
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

    void updateMovedConnections(MindMap map) {
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
            contextMenuFactory.apply(child).show(shape, event.getScreenX(), event.getScreenY());
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
}
