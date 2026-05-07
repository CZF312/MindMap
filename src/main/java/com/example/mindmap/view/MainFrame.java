package com.example.mindmap.view;

import com.example.mindmap.controller.MindMapController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class MainFrame extends BorderPane {
    private static final double EXPANDED_STRUCTURE_MIN_WIDTH = 260;
    private static final double EXPANDED_STRUCTURE_PREF_WIDTH = 340;
    private static final double COLLAPSED_STRUCTURE_WIDTH = 42;

    private final ToolbarPanel toolbarPanel;
    private final MindMapCanvas canvas;
    private final MindMapTreePanel treePanel;
    private final SplitPane splitPane;
    private final BorderPane structureContainer = new BorderPane();
    private final BorderPane collapsedStructureRail = new BorderPane();
    private final Label statusLabel = new Label("就绪");
    private final Button resetZoomButton = new Button("100%");
    private double expandedDividerPosition = 0.76;
    private boolean structureCollapsed;

    public MainFrame(MindMapController controller) {
        getStyleClass().add("app-root");
        toolbarPanel = new ToolbarPanel(controller);
        canvas = new MindMapCanvas(controller);
        treePanel = new MindMapTreePanel(controller, this::collapseStructurePanel);
        configureStructurePanel();

        splitPane = new SplitPane(canvas, structureContainer);
        splitPane.setDividerPositions(0.76);
        splitPane.getStyleClass().add("workspace-split");

        HBox zoomControls = zoomControls(controller);
        HBox statusBar = new HBox(12, statusLabel, zoomControls);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new Insets(8, 14, 8, 14));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        setTop(toolbarPanel);
        setCenter(splitPane);
        setBottom(statusBar);
    }

    private void configureStructurePanel() {
        structureContainer.getStyleClass().add("structure-container");
        structureContainer.setCenter(treePanel);
        structureContainer.setMinWidth(EXPANDED_STRUCTURE_MIN_WIDTH);
        structureContainer.setPrefWidth(EXPANDED_STRUCTURE_PREF_WIDTH);
        structureContainer.setMaxWidth(Double.MAX_VALUE);

        Button expandButton = new Button("<\n结构");
        expandButton.getStyleClass().add("structure-rail-button");
        expandButton.setTooltip(new Tooltip("展开结构栏"));
        expandButton.setOnAction(event -> expandStructurePanel());
        collapsedStructureRail.getStyleClass().add("structure-rail");
        collapsedStructureRail.setCenter(expandButton);
        BorderPane.setAlignment(expandButton, Pos.TOP_CENTER);
    }

    private void collapseStructurePanel() {
        if (structureCollapsed) {
            return;
        }
        if (!splitPane.getDividers().isEmpty()) {
            expandedDividerPosition = clampDivider(splitPane.getDividers().get(0).getPosition());
        }
        structureCollapsed = true;
        structureContainer.setCenter(collapsedStructureRail);
        structureContainer.setMinWidth(COLLAPSED_STRUCTURE_WIDTH);
        structureContainer.setPrefWidth(COLLAPSED_STRUCTURE_WIDTH);
        structureContainer.setMaxWidth(COLLAPSED_STRUCTURE_WIDTH);
        splitPane.setDividerPositions(1.0);
        setStatus("已收起结构栏");
    }

    private void expandStructurePanel() {
        if (!structureCollapsed) {
            return;
        }
        structureCollapsed = false;
        structureContainer.setMaxWidth(Double.MAX_VALUE);
        structureContainer.setMinWidth(EXPANDED_STRUCTURE_MIN_WIDTH);
        structureContainer.setPrefWidth(EXPANDED_STRUCTURE_PREF_WIDTH);
        structureContainer.setCenter(treePanel);
        splitPane.setDividerPositions(expandedDividerPosition);
        setStatus("已展开结构栏");
    }

    private double clampDivider(double position) {
        return Math.max(0.55, Math.min(0.9, position));
    }

    private HBox zoomControls(MindMapController controller) {
        Button zoomOutButton = statusButton("-", "缩小", controller::zoomOut);
        Button zoomInButton = statusButton("+", "放大", controller::zoomIn);
        Button fitButton = statusButton("适应", "适应窗口", controller::fitToWindow);
        resetZoomButton.getStyleClass().add("status-zoom-button");
        resetZoomButton.setTooltip(new Tooltip("恢复实际大小"));
        resetZoomButton.setOnAction(event -> controller.resetZoom());
        HBox controls = new HBox(6, zoomOutButton, resetZoomButton, zoomInButton, fitButton);
        controls.getStyleClass().add("zoom-controls");
        controls.setAlignment(Pos.CENTER_RIGHT);
        return controls;
    }

    private Button statusButton(String text, String tooltip, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("status-zoom-button");
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(event -> action.run());
        return button;
    }

    public ToolbarPanel getToolbarPanel() {
        return toolbarPanel;
    }

    public MindMapCanvas getCanvas() {
        return canvas;
    }

    public MindMapTreePanel getTreePanel() {
        return treePanel;
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void setZoom(double zoom) {
        String text = Math.round(zoom * 100) + "%";
        resetZoomButton.setText(text);
    }
}
