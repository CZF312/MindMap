package com.example.mindmap.view;

import com.example.mindmap.controller.MindMapController;
import com.example.mindmap.model.LayoutType;
import javafx.geometry.Insets;
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

import java.nio.file.Path;
import java.util.List;

public class ToolbarPanel extends VBox {
    private final MindMapController controller;
    private final Menu recentMenu = new Menu("最近打开");
    private final Button saveButton = button("保存", "保存当前导图");
    private final Button addSiblingButton = button("兄弟", "为主选中节点添加兄弟节点");
    private final Button deleteButton = button("删除", "删除选中节点");
    private final Button renameButton = button("重命名", "修改主选中节点文本");
    private final Button undoButton = button("撤销", "撤销上一次编辑");
    private final Button redoButton = button("重做", "重做上一次撤销");
    private final Button collapseButton = button("折叠", "折叠或展开主选中节点");
    private final TextField searchField = new TextField();
    private final TextField replaceField = new TextField();
    private final ColorPicker fillPicker = new ColorPicker(Color.WHITE);
    private final ColorPicker borderPicker = new ColorPicker(Color.web("#CBD5E1"));
    private final ColorPicker textPicker = new ColorPicker(Color.web("#0F172A"));
    private final ColorPicker canvasPicker = new ColorPicker(Color.WHITE);
    private final ComboBox<String> fontBox = new ComboBox<>();
    private final ComboBox<Integer> fontSizeBox = new ComboBox<>();
    private final ToggleButton autoLayout = layoutButton("自动");
    private final ToggleButton leftLayout = layoutButton("左侧");
    private final ToggleButton rightLayout = layoutButton("右侧");
    private final ToggleButton startTab = ribbonTab("开始");
    private final ToggleButton styleTab = ribbonTab("样式");
    private final ToggleButton layoutTab = ribbonTab("布局");
    private final ToggleButton exportTab = ribbonTab("导出");
    private boolean updatingControls;

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
        fillPicker.setMinWidth(120);
        borderPicker.setMinWidth(120);
        textPicker.setMinWidth(105);
        canvasPicker.setMinWidth(120);
        fillPicker.setOnAction(event -> controller.changeFillColor(fillPicker.getValue()));
        borderPicker.setOnAction(event -> controller.changeBorderColor(borderPicker.getValue()));
        textPicker.setOnAction(event -> controller.changeTextColor(textPicker.getValue()));
        canvasPicker.setOnAction(event -> {
            if (!updatingControls) {
                controller.changeCanvasColor(canvasPicker.getValue());
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

        HBox startPage = ribbonPage(
                ribbonGroup("文字", fontBox, fontSizeBox,
                        iconButton("B", "加粗", controller::toggleBold),
                        iconButton("I", "斜体", controller::toggleItalic),
                        iconButton("U", "下划线", controller::toggleUnderline),
                        iconButton("S", "删除线", controller::toggleStrikethrough),
                        textPicker,
                        iconButton("≡", "左对齐", () -> controller.changeTextAlignment(javafx.scene.text.TextAlignment.LEFT)),
                        iconButton("≣", "居中对齐", () -> controller.changeTextAlignment(javafx.scene.text.TextAlignment.CENTER)),
                        iconButton("≡", "右对齐", () -> controller.changeTextAlignment(javafx.scene.text.TextAlignment.RIGHT))),
                ribbonGroup("颜色", fillPicker, borderPicker),
                ribbonGroup("节点",
                        button("子节点", "添加子节点", controller::addChildNode),
                        addSiblingButton,
                        renameButton,
                        deleteButton,
                        collapseButton,
                        button("全部展开", "展开所有节点", controller::expandAll),
                        button("全部收起", "收起所有分支", controller::collapseAll)),
                ribbonGroup("编辑", undoButton, redoButton),
                ribbonGroup("查找", button("⌕ 查找替换", "打开查找替换窗口", controller::showFindReplaceDialog))
        );
        HBox stylePage = ribbonPage(
                ribbonGroup("画布", canvasPicker),
                ribbonGroup("常用样式",
                        button("白底", "设置选中节点为白色填充", () -> controller.changeFillColor(Color.WHITE)),
                        button("蓝底", "设置选中节点为蓝色填充", () -> controller.changeFillColor(Color.web("#2563EB"))),
                        button("浅黄", "设置选中节点为浅黄色填充", () -> controller.changeFillColor(Color.web("#FEF3C7"))))
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
                        button("JPG", "导出 JPG 图片", controller::exportJpg))
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
        HBox ribbonHeader = new HBox(12, fileButton, leftSpacer, tabRow, rightSpacer);
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

    public void updateState(boolean hasSelection, boolean canAddSibling, boolean canDelete, boolean canCollapse,
                            boolean canUndo, boolean canRedo, LayoutType layoutType) {
        addSiblingButton.setDisable(!canAddSibling);
        deleteButton.setDisable(!canDelete);
        renameButton.setDisable(!hasSelection);
        collapseButton.setDisable(!canCollapse);
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
