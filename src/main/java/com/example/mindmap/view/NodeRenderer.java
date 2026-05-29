package com.example.mindmap.view;

import com.example.mindmap.controller.MindMapController;
import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;
import com.example.mindmap.model.NodeStyle;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.scene.transform.Shear;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class NodeRenderer {
    private final Group contentGroup;
    private final MindMapController controller;
    private final Runnable connectionUpdater;
    private final ContextMenuPresenter contextMenuPresenter;
    private final Map<String, StackPane> nodeViews = new HashMap<>();
    private MindMap map;
    private double dragLastX;
    private double dragLastY;
    private boolean dragging;

    private enum ResizeHandle {
        TOP_LEFT, TOP, TOP_RIGHT, RIGHT, BOTTOM_RIGHT, BOTTOM, BOTTOM_LEFT, LEFT
    }

    NodeRenderer(Group contentGroup, MindMapController controller, Runnable connectionUpdater,
                 ContextMenuPresenter contextMenuPresenter) {
        this.contentGroup = contentGroup;
        this.controller = controller;
        this.connectionUpdater = connectionUpdater;
        this.contextMenuPresenter = contextMenuPresenter;
    }

    void clear() {
        nodeViews.clear();
    }

    void draw(MindMap map, Set<String> selectedIds, Set<String> searchIds, String searchKeyword) {
        this.map = map;
        clear();
        // 只绘制可见节点；被折叠的子节点仍保留在模型中。
        for (MindNode node : map.visibleNodes()) {
            drawNode(node, selectedIds.contains(node.getId()), searchIds.contains(node.getId()), searchKeyword);
        }
    }

    StackPane getNodeView(String nodeId) {
        return nodeViews.get(nodeId);
    }

    private void drawNode(MindNode node, boolean selected, boolean searchHit, String searchKeyword) {
        StackPane box = new StackPane();
        box.getStyleClass().add(node.isRoot() ? "mind-node-root" : "mind-node");
        if (selected) {
            box.getStyleClass().add("mind-node-selected");
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

        TextFlow textFlow = createTextFlow(node, searchHit, searchKeyword);
        box.getChildren().add(textFlow);
        StackPane.setMargin(textFlow, new Insets(8, 14, 8, 14));
        StackPane.setAlignment(textFlow, alignment(node.getStyle().getAlignment()));
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
                connectionUpdater.run();
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
                contextMenuPresenter.show(contextMenu(node), box, event.getScreenX(), event.getScreenY());
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

    private TextFlow createTextFlow(MindNode node, boolean searchHit, String searchKeyword) {
        TextFlow flow = new TextFlow();
        double textWidth = Math.max(76, node.getWidth() - 28);
        flow.setPrefWidth(textWidth);
        flow.setMaxWidth(textWidth);
        flow.setTextAlignment(node.getStyle().getAlignment());
        String text = node.getText() == null ? "" : node.getText();
        String keyword = searchKeyword == null ? "" : searchKeyword.trim();
        if (!searchHit || keyword.isEmpty()) {
            flow.getChildren().add(normalText(node, text));
            return flow;
        }
        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        int cursor = 0;
        int match = lowerText.indexOf(lowerKeyword);
        while (match >= 0) {
            if (match > cursor) {
                flow.getChildren().add(normalText(node, text.substring(cursor, match)));
            }
            flow.getChildren().add(highlightText(node, text.substring(match, match + keyword.length())));
            cursor = match + keyword.length();
            match = lowerText.indexOf(lowerKeyword, cursor);
        }
        if (cursor < text.length()) {
            flow.getChildren().add(normalText(node, text.substring(cursor)));
        }
        return flow;
    }

    private Text normalText(MindNode node, String value) {
        Text text = new Text(value);
        applyTextStyle(node, text);
        return text;
    }

    private Label highlightText(MindNode node, String value) {
        Label label = new Label(value);
        label.getStyleClass().add("mind-node-search-text");
        label.setTextFill(node.getStyle().getTextColor());
        label.setFont(textFont(node));
        label.setUnderline(node.getStyle().isUnderline());
        if (node.getStyle().isItalic()) {
            label.getTransforms().add(new Shear(-0.22, 0));
        }
        return label;
    }

    private void applyTextStyle(MindNode node, Text text) {
        text.setFill(node.getStyle().getTextColor());
        text.setUnderline(node.getStyle().isUnderline());
        text.setStrikethrough(node.getStyle().isStrikethrough());
        text.setFont(textFont(node));
        if (node.getStyle().isItalic()) {
            text.getTransforms().add(new Shear(-0.22, 0));
        }
    }

    private Font textFont(MindNode node) {
        return Font.font(node.getStyle().getFontFamily(),
                node.getStyle().isBold() ? FontWeight.BOLD : FontWeight.NORMAL,
                node.getStyle().isItalic() ? FontPosture.ITALIC : FontPosture.REGULAR,
                node.getStyle().getFontSize());
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
                connectionUpdater.run();
                updateResizeHandles();
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
        updateResizeHandles();
    }

    private void updateResizeHandles() {
        if (map == null) {
            return;
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

    private MenuItem menuItem(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> action.run());
        return item;
    }
}
