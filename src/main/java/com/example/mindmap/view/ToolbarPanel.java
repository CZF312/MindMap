package com.example.mindmap.view;

import com.example.mindmap.controller.MindMapController;
import com.example.mindmap.model.ConnectionShape;
import com.example.mindmap.model.LayoutType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.nio.file.Path;
import java.util.List;

public class ToolbarPanel extends VBox {
    private final MindMapController controller;
    private final Menu recentMenu = new Menu("最近打开");
    private final Button saveButton = button("保存", "保存当前导图");
    private final Button addSiblingButton = button("兄弟", "为主选中节点添加兄弟节点");
    private final Button deleteButton = button("删除", "删除选中节点");
    private final Button renameButton = button("重命名", "修改主选中节点文本");
    private final Button undoButton = button("", "撤销上一次编辑");
    private final Button redoButton = button("", "重做上一次撤销");
    private final Button collapseButton = button("折叠", "折叠或展开主选中节点");
    private final TextField searchField = new TextField();
    private final TextField replaceField = new TextField();
    private final ColorPicker fillPicker = new ColorPicker(Color.WHITE);
    private final ColorPicker borderPicker = new ColorPicker(Color.web("#CBD5E1"));
    private final ColorPicker textPicker = new ColorPicker(Color.web("#0F172A"));
    private final ColorPicker canvasPicker = new ColorPicker(Color.WHITE);
    private final ColorPicker connectionPicker = new ColorPicker(Color.web("#94A3B8"));
    private final ComboBox<String> fontBox = new ComboBox<>();
    private final ComboBox<Integer> fontSizeBox = new ComboBox<>();
    private final ComboBox<Double> connectionWidthBox = new ComboBox<>();
    private final ToggleButton boldButton = textStyleButton(TextStyleIcon.BOLD, "加粗");
    private final ToggleButton italicButton = textStyleButton(TextStyleIcon.ITALIC, "斜体");
    private final ToggleButton underlineButton = textStyleButton(TextStyleIcon.UNDERLINE, "下划线");
    private final ToggleButton strikethroughButton = textStyleButton(TextStyleIcon.STRIKETHROUGH, "删除线");
    private final ToggleButton connectionDashedButton = new ToggleButton("虚线");
    private final ToggleButton curveConnectionButton = layoutButton("曲线");
    private final ToggleButton elbowConnectionButton = layoutButton("折线");
    private final ToggleButton autoLayout = layoutButton("自动");
    private final ToggleButton leftLayout = layoutButton("左侧");
    private final ToggleButton rightLayout = layoutButton("右侧");
    private final ToggleButton startTab = ribbonTab("开始");
    private final ToggleButton styleTab = ribbonTab("样式");
    private final ToggleButton layoutTab = ribbonTab("布局");
    private final ToggleButton exportTab = ribbonTab("导出");
    private boolean updatingControls;

    private enum TextAlignIcon {
        LEFT, CENTER, RIGHT
    }

    private enum TextStyleIcon {
        BOLD, ITALIC, UNDERLINE, STRIKETHROUGH
    }

    public ToolbarPanel(MindMapController controller) {
        this.controller = controller;
        getStyleClass().add("toolbar-panel");

        MenuButton fileButton = new MenuButton("文件");
        fileButton.getStyleClass().add("app-file-button");
        fileButton.getItems().addAll(
                item("新建", controller::newMap),
                item("打开...", controller::openMap),
                item("保存", controller::saveMap),
                item("另存为...", controller::saveMapAs),
                item("导出 PNG...", controller::exportPng),
                item("导出 JPG...", controller::exportJpg),
                item("导出 PDF...", controller::exportPdf),
                recentMenu);

        ToggleGroup layoutGroup = new ToggleGroup();
        autoLayout.setToggleGroup(layoutGroup);
        leftLayout.setToggleGroup(layoutGroup);
        rightLayout.setToggleGroup(layoutGroup);

        searchField.setPromptText("搜索节点");
        searchField.getStyleClass().add("search-field");
        searchField.setMinWidth(160);
        searchField.textProperty().addListener((obs, oldValue, newValue) -> controller.search(newValue));
        searchField.setOnAction(event -> controller.nextSearchResult());

        replaceField.setPromptText("替换为");
        replaceField.getStyleClass().add("replace-field");
        replaceField.setMinWidth(130);
        replaceField.setOnAction(event -> controller.replaceCurrentSearchResult(searchField.getText(), replaceField.getText()));

        fillPicker.setTooltip(new javafx.scene.control.Tooltip("填充色"));
        borderPicker.setTooltip(new javafx.scene.control.Tooltip("边框色"));
        textPicker.setTooltip(new javafx.scene.control.Tooltip("文字颜色"));
        canvasPicker.setTooltip(new javafx.scene.control.Tooltip("画布背景色"));
        connectionPicker.setTooltip(new javafx.scene.control.Tooltip("连接线颜色"));
        fillPicker.setMinWidth(120);
        borderPicker.setMinWidth(120);
        textPicker.setMinWidth(105);
        HBox fillColorControl = labeledColorPicker(fillPicker, "节点背景", "■", "node-background-picker");
        HBox borderColorControl = labeledColorPicker(borderPicker, "节点边框", "□", "node-border-picker");
        HBox textColorControl = labeledColorPicker(textPicker, "文字颜色", "A", "text-color-picker");
        canvasPicker.setMinWidth(120);
        connectionPicker.setMinWidth(120);
        fillPicker.setOnAction(event -> {
            if (!updatingControls) {
                controller.changeFillColor(fillPicker.getValue());
            }
        });
        borderPicker.setOnAction(event -> {
            if (!updatingControls) {
                controller.changeBorderColor(borderPicker.getValue());
            }
        });
        textPicker.setOnAction(event -> {
            if (!updatingControls) {
                controller.changeTextColor(textPicker.getValue());
            }
        });
        canvasPicker.setOnAction(event -> {
            if (!updatingControls) {
                controller.changeCanvasColor(canvasPicker.getValue());
            }
        });
        connectionPicker.setOnAction(event -> {
            if (!updatingControls) {
                controller.changeConnectionColor(connectionPicker.getValue());
            }
        });
        fontBox.getItems().addAll("Microsoft YaHei UI", "SimSun", "SimHei", "KaiTi", "Arial");
        fontBox.setValue("Microsoft YaHei UI");
        fontBox.setTooltip(new javafx.scene.control.Tooltip("字体"));
        fontBox.setMinWidth(150);
        fontBox.setOnAction(event -> controller.changeFontFamily(fontBox.getValue()));
        fontSizeBox.getItems().addAll(12, 14, 16, 18, 20, 24, 28, 30, 36, 48);
        fontSizeBox.setValue(13);
        fontSizeBox.setTooltip(new javafx.scene.control.Tooltip("字号"));
        fontSizeBox.setMinWidth(82);
        fontSizeBox.setOnAction(event -> controller.changeFontSize(fontSizeBox.getValue()));
        boldButton.setOnAction(event -> {
            if (!updatingControls) {
                controller.toggleBold();
            }
        });
        italicButton.setOnAction(event -> {
            if (!updatingControls) {
                controller.toggleItalic();
            }
        });
        underlineButton.setOnAction(event -> {
            if (!updatingControls) {
                controller.toggleUnderline();
            }
        });
        strikethroughButton.setOnAction(event -> {
            if (!updatingControls) {
                controller.toggleStrikethrough();
            }
        });
        connectionWidthBox.getItems().addAll(1.0, 1.5, 2.0, 2.2, 3.0, 4.0, 6.0, 8.0);
        connectionWidthBox.setValue(2.2);
        connectionWidthBox.setTooltip(new javafx.scene.control.Tooltip("连接线粗细"));
        connectionWidthBox.setMinWidth(86);
        connectionWidthBox.setOnAction(event -> {
            if (!updatingControls && connectionWidthBox.getValue() != null) {
                controller.changeConnectionWidth(connectionWidthBox.getValue());
            }
        });
        connectionDashedButton.getStyleClass().add("tool-button");
        connectionDashedButton.setTooltip(new javafx.scene.control.Tooltip("实线 / 虚线"));
        connectionDashedButton.setMinWidth(Region.USE_PREF_SIZE);
        connectionDashedButton.setOnAction(event -> {
            if (!updatingControls) {
                controller.toggleConnectionDashed();
            }
        });
        ToggleGroup connectionShapeGroup = new ToggleGroup();
        curveConnectionButton.setToggleGroup(connectionShapeGroup);
        elbowConnectionButton.setToggleGroup(connectionShapeGroup);
        curveConnectionButton.setTooltip(new javafx.scene.control.Tooltip("曲线连接"));
        elbowConnectionButton.setTooltip(new javafx.scene.control.Tooltip("折线连接"));
        curveConnectionButton.setOnAction(event -> {
            if (!updatingControls) {
                controller.changeConnectionShape(ConnectionShape.CURVE);
            }
        });
        elbowConnectionButton.setOnAction(event -> {
            if (!updatingControls) {
                controller.changeConnectionShape(ConnectionShape.ELBOW);
            }
        });

        HBox startPage = ribbonPage(
                ribbonGroup("文字", fontBox, fontSizeBox,
                        boldButton,
                        italicButton,
                        underlineButton,
                        strikethroughButton,
                        textColorControl,
                        alignmentButton(TextAlignIcon.LEFT, "左对齐", () -> controller.changeTextAlignment(javafx.scene.text.TextAlignment.LEFT)),
                        alignmentButton(TextAlignIcon.CENTER, "居中对齐", () -> controller.changeTextAlignment(javafx.scene.text.TextAlignment.CENTER)),
                        alignmentButton(TextAlignIcon.RIGHT, "右对齐", () -> controller.changeTextAlignment(javafx.scene.text.TextAlignment.RIGHT))),
                ribbonGroup("颜色", fillColorControl, borderColorControl),
                ribbonGroup("节点",
                        button("子节点", "添加子节点", controller::addChildNode),
                        addSiblingButton,
                        renameButton,
                        deleteButton,
                        collapseButton,
                        button("全部展开", "展开所有节点", controller::expandAll),
                        button("全部收起", "收起所有分支", controller::collapseAll)),
                ribbonGroup("查找", button("⌕ 查找替换", "打开查找替换窗口", controller::showFindReplaceDialog))
        );
        HBox stylePage = ribbonPage(
                ribbonGroup("画布", canvasPicker),
                ribbonGroup("连接线", connectionPicker, connectionWidthBox, connectionDashedButton,
                        curveConnectionButton, elbowConnectionButton)
        );
        HBox layoutPage = ribbonPage(
                ribbonGroup("分布方式", autoLayout, leftLayout, rightLayout)
        );
        HBox exportPage = ribbonPage(
                ribbonGroup("文件",
                        button("新建", "新建思维导图", controller::newMap),
                        button("打开", "打开 .mindmap 文件", controller::openMap),
                        saveButton,
                        button("另存为", "另存为 .mindmap 文件", controller::saveMapAs)),
                ribbonGroup("导出",
                        button("PNG", "导出 PNG 图片", controller::exportPng),
                        button("JPG", "导出 JPG 图片", controller::exportJpg),
                        button("PDF", "导出 PDF 文档", controller::exportPdf))
        );

        StackPane ribbonPages = new StackPane(startPage, stylePage, layoutPage, exportPage);
        ribbonPages.getStyleClass().add("ribbon-pages");
        showRibbonPage(startPage, startPage, stylePage, layoutPage, exportPage);

        ToggleGroup ribbonGroup = new ToggleGroup();
        startTab.setToggleGroup(ribbonGroup);
        styleTab.setToggleGroup(ribbonGroup);
        layoutTab.setToggleGroup(ribbonGroup);
        exportTab.setToggleGroup(ribbonGroup);
        startTab.setSelected(true);
        startTab.setOnAction(event -> showRibbonPage(startPage, startPage, stylePage, layoutPage, exportPage));
        styleTab.setOnAction(event -> showRibbonPage(stylePage, startPage, stylePage, layoutPage, exportPage));
        layoutTab.setOnAction(event -> showRibbonPage(layoutPage, startPage, stylePage, layoutPage, exportPage));
        exportTab.setOnAction(event -> showRibbonPage(exportPage, startPage, stylePage, layoutPage, exportPage));

        HBox tabRow = new HBox(4, startTab, styleTab, layoutTab, exportTab);
        tabRow.getStyleClass().add("ribbon-tabs");
        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);
        Region quickSeparator = new Region();
        quickSeparator.getStyleClass().add("quick-action-separator");
        HBox quickActions = new HBox(4, quickSeparator, undoButton, redoButton);
        quickActions.getStyleClass().add("quick-actions");
        HBox ribbonHeader = new HBox(12, fileButton, quickActions, leftSpacer, tabRow, rightSpacer);
        ribbonHeader.getStyleClass().add("ribbon-header");

        ScrollPane toolScroller = new ScrollPane(ribbonPages);
        toolScroller.getStyleClass().add("tool-scroller");
        toolScroller.setFitToHeight(true);
        toolScroller.setFitToWidth(false);
        toolScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        toolScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        toolScroller.setPannable(true);

        saveButton.setOnAction(event -> controller.saveMap());
        addSiblingButton.setOnAction(event -> controller.addSiblingNode());
        deleteButton.setOnAction(event -> controller.deleteSelectedNodes());
        renameButton.setOnAction(event -> controller.renameSelectedNode());
        undoButton.getStyleClass().add("quick-action-button");
        redoButton.getStyleClass().add("quick-action-button");
        undoButton.setGraphic(quickActionIcon(false));
        redoButton.setGraphic(quickActionIcon(true));
        undoButton.setOnAction(event -> controller.undo());
        redoButton.setOnAction(event -> controller.redo());
        collapseButton.setOnAction(event -> controller.toggleCollapse());
        autoLayout.setOnAction(event -> controller.changeLayout(LayoutType.AUTO));
        leftLayout.setOnAction(event -> controller.changeLayout(LayoutType.LEFT));
        rightLayout.setOnAction(event -> controller.changeLayout(LayoutType.RIGHT));

        getChildren().addAll(ribbonHeader, toolScroller);
    }

    public TextField getSearchField() {
        return searchField;
    }

    public TextField getReplaceField() {
        return replaceField;
    }

    public void focusSearchField() {
        searchField.requestFocus();
        searchField.selectAll();
    }

    public void focusReplaceField() {
        replaceField.requestFocus();
        replaceField.selectAll();
    }

    public void updateCanvasColor(Color color) {
        updatingControls = true;
        canvasPicker.setValue(color == null ? Color.WHITE : color);
        updatingControls = false;
    }

    public void updateNodeStyle(Color fillColor, Color borderColor, Color textColor) {
        updatingControls = true;
        fillPicker.setValue(fillColor == null ? Color.WHITE : fillColor);
        borderPicker.setValue(borderColor == null ? Color.web("#CBD5E1") : borderColor);
        textPicker.setValue(textColor == null ? Color.web("#0F172A") : textColor);
        updatingControls = false;
    }

    public void updateTextStyle(boolean bold, boolean italic, boolean underline, boolean strikethrough) {
        updatingControls = true;
        boldButton.setSelected(bold);
        italicButton.setSelected(italic);
        underlineButton.setSelected(underline);
        strikethroughButton.setSelected(strikethrough);
        updatingControls = false;
    }

    public void updateConnectionStyle(Color color, double width, boolean dashed, ConnectionShape shape) {
        updatingControls = true;
        connectionPicker.setValue(color == null ? Color.web("#94A3B8") : color);
        connectionWidthBox.setValue(width);
        connectionDashedButton.setSelected(dashed);
        connectionDashedButton.setText(dashed ? "虚线" : "实线");
        if (shape == ConnectionShape.ELBOW) {
            elbowConnectionButton.setSelected(true);
        } else {
            curveConnectionButton.setSelected(true);
        }
        updatingControls = false;
    }

    public void updateState(boolean hasSelection, boolean canAddSibling, boolean canDelete, boolean canCollapse,
                            boolean canUndo, boolean canRedo, LayoutType layoutType) {
        addSiblingButton.setDisable(!canAddSibling);
        deleteButton.setDisable(!canDelete);
        renameButton.setDisable(!hasSelection);
        collapseButton.setDisable(!canCollapse);
        boldButton.setDisable(!hasSelection);
        italicButton.setDisable(!hasSelection);
        underlineButton.setDisable(!hasSelection);
        strikethroughButton.setDisable(!hasSelection);
        undoButton.setDisable(!canUndo);
        redoButton.setDisable(!canRedo);
        if (layoutType == LayoutType.LEFT) {
            leftLayout.setSelected(true);
        } else if (layoutType == LayoutType.RIGHT) {
            rightLayout.setSelected(true);
        } else {
            autoLayout.setSelected(true);
        }
    }

    public void updateRecentFiles(List<Path> files) {
        recentMenu.getItems().clear();
        if (files.isEmpty()) {
            MenuItem empty = new MenuItem("暂无记录");
            empty.setDisable(true);
            recentMenu.getItems().add(empty);
            return;
        }
        for (Path file : files) {
            MenuItem item = new MenuItem(file.toString());
            item.setOnAction(event -> controller.openRecentFile(file));
            recentMenu.getItems().add(item);
        }
    }

    private MenuItem item(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> action.run());
        return item;
    }

    private HBox labeledColorPicker(ColorPicker picker, String text, String icon, String styleClass) {
        picker.setAccessibleText(text);
        picker.getStyleClass().add("compact-color-picker");
        picker.setStyle("-fx-color-label-visible: false;");
        picker.setMinWidth(42);
        picker.setPrefWidth(42);
        picker.setMaxWidth(42);
        Label label = new Label(icon + "  " + text);
        label.getStyleClass().add("labeled-color-text");
        HBox box = new HBox(6, label, picker);
        box.getStyleClass().addAll("labeled-color-picker", styleClass);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMinWidth(142);
        box.setPrefWidth(142);
        box.setOnMouseClicked(event -> {
            if (!isInsidePicker(event.getTarget(), picker)) {
                picker.show();
            }
        });
        return box;
    }

    private boolean isInsidePicker(Object target, ColorPicker picker) {
        if (!(target instanceof Node node)) {
            return false;
        }
        while (node != null) {
            if (node == picker) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private Button button(String text, String tooltip) {
        return button(text, tooltip, null);
    }

    private Button button(String text, String tooltip, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("tool-button");
        button.setMinWidth(Region.USE_PREF_SIZE);
        button.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        if (action != null) {
            button.setOnAction(event -> action.run());
        }
        return button;
    }

    private Button iconButton(String text, String tooltip, Runnable action) {
        Button button = button(text, tooltip, action);
        button.getStyleClass().add("icon-tool-button");
        return button;
    }

    private ToggleButton textStyleButton(TextStyleIcon icon, String tooltip) {
        ToggleButton button = new ToggleButton();
        button.getStyleClass().add("text-style-button");
        button.setGraphic(textStyleIcon(icon));
        button.setMinSize(36, 36);
        button.setPrefSize(36, 36);
        button.setMaxSize(36, 36);
        button.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        button.setAccessibleText(tooltip);
        return button;
    }

    private Node textStyleIcon(TextStyleIcon icon) {
        Text text = new Text(switch (icon) {
            case BOLD -> "B";
            case ITALIC -> "I";
            case UNDERLINE -> "U";
            case STRIKETHROUGH -> "S";
        });
        text.getStyleClass().add("text-style-icon");
        text.setFont(Font.font("Microsoft YaHei UI",
                icon == TextStyleIcon.BOLD ? FontWeight.BOLD : FontWeight.NORMAL,
                icon == TextStyleIcon.ITALIC ? FontPosture.ITALIC : FontPosture.REGULAR,
                17));
        text.setUnderline(icon == TextStyleIcon.UNDERLINE);
        text.setStrikethrough(icon == TextStyleIcon.STRIKETHROUGH);
        text.setMouseTransparent(true);
        return text;
    }

    private Node quickActionIcon(boolean redo) {
        SVGPath icon = new SVGPath();
        icon.setContent(redo
                ? "M 15 6 L 20 11 L 15 16 M 19 11 H 10 C 6 11 4 14 4 18"
                : "M 9 6 L 4 11 L 9 16 M 5 11 H 14 C 18 11 20 14 20 18");
        icon.setFill(Color.TRANSPARENT);
        icon.setStroke(Color.web("#64748B"));
        icon.setStrokeWidth(2.2);
        icon.setStrokeLineCap(StrokeLineCap.ROUND);
        icon.setStrokeLineJoin(StrokeLineJoin.ROUND);
        icon.setMouseTransparent(true);
        return icon;
    }

    private Button alignmentButton(TextAlignIcon icon, String tooltip, Runnable action) {
        Button button = iconButton("", tooltip, action);
        button.getStyleClass().add("alignment-tool-button");
        button.setGraphic(alignmentIcon(icon));
        return button;
    }

    private Node alignmentIcon(TextAlignIcon icon) {
        VBox lines = new VBox(4);
        lines.getStyleClass().add("align-icon");
        lines.setMinSize(22, 20);
        lines.setPrefSize(22, 20);
        lines.setMaxSize(22, 20);
        lines.setAlignment(switch (icon) {
            case LEFT -> Pos.CENTER_LEFT;
            case CENTER -> Pos.CENTER;
            case RIGHT -> Pos.CENTER_RIGHT;
        });
        lines.getChildren().addAll(alignLine(18), alignLine(12), alignLine(18));
        return lines;
    }

    private Region alignLine(double width) {
        Region line = new Region();
        line.getStyleClass().add("align-icon-line");
        line.setMinSize(width, 2);
        line.setPrefSize(width, 2);
        line.setMaxSize(width, 2);
        return line;
    }

    private ToggleButton ribbonTab(String text) {
        ToggleButton button = new ToggleButton(text);
        button.getStyleClass().add("ribbon-tab");
        button.setMinWidth(Region.USE_PREF_SIZE);
        return button;
    }

    private HBox ribbonPage(VBox... groups) {
        HBox page = new HBox(10, groups);
        page.getStyleClass().add("tool-row");
        page.setPadding(new Insets(8, 12, 8, 12));
        page.setMinWidth(Region.USE_PREF_SIZE);
        return page;
    }

    private VBox ribbonGroup(String title, Region... controls) {
        HBox row = new HBox(6, controls);
        row.getStyleClass().add("ribbon-group-row");
        Label label = new Label(title);
        label.getStyleClass().add("ribbon-group-title");
        label.setMaxWidth(Double.MAX_VALUE);
        VBox group = new VBox(4, row, label);
        group.getStyleClass().add("ribbon-group");
        return group;
    }

    private void showRibbonPage(HBox activePage, HBox... pages) {
        for (HBox page : pages) {
            boolean active = page == activePage;
            page.setVisible(active);
            page.setManaged(active);
        }
    }

    private ToggleButton layoutButton(String text) {
        ToggleButton button = new ToggleButton(text);
        button.getStyleClass().add("layout-button");
        button.setMinWidth(Region.USE_PREF_SIZE);
        return button;
    }
}
