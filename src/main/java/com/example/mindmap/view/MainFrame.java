package com.example.mindmap.view;

import com.example.mindmap.controller.MindMapController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

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
    private final Label wordCountLabel = new Label("字数：0");
    private final Label topicCountLabel = new Label("主题数：0");
    private final Slider zoomSlider = new Slider(30, 300, 100);
    private final Region zoomProgressFill = new Region();
    private final Button resetZoomButton = new Button("100%");
    private double expandedDividerPosition = 0.76;
    private boolean structureCollapsed;
    private boolean updatingZoomControl;

    public MainFrame(MindMapController controller) {
        getStyleClass().add("app-root");
        toolbarPanel = new ToolbarPanel(controller);
        canvas = new MindMapCanvas(controller);
        treePanel = new MindMapTreePanel(controller, this::collapseStructurePanel);
        configureStructurePanel();

        splitPane = new SplitPane(canvas, structureContainer);
        splitPane.setDividerPositions(0.76);
        splitPane.getStyleClass().add("workspace-split");

        HBox stats = documentStats();
        HBox zoomControls = zoomControls(controller);
        Region spacer = new Region();
        HBox statusBar = new HBox(12, statusLabel, spacer, stats, zoomControls);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new Insets(8, 14, 8, 14));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(spacer, Priority.ALWAYS);

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

        Button expandButton = new Button("<");
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
        Button zoomOutButton = zoomStepButton("-", "缩小", -10, controller);
        Button zoomInButton = zoomStepButton("+", "放大", 10, controller);
        zoomOutButton.getStyleClass().add("status-step-button");
        zoomInButton.getStyleClass().add("status-step-button");
        zoomSlider.getStyleClass().add("zoom-slider");
        zoomSlider.setTooltip(new Tooltip("拖动调整缩放比例"));
        StackPane zoomSliderControl = zoomSliderControl();
        zoomSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!updatingZoomControl) {
                controller.changeZoom(newValue.doubleValue() / 100.0);
            }
        });
        resetZoomButton.getStyleClass().add("status-zoom-button");
        resetZoomButton.getStyleClass().add("status-percent-button");
        resetZoomButton.setTooltip(new Tooltip("恢复实际大小"));
        resetZoomButton.setOnAction(event -> controller.resetZoom());
        HBox controls = new HBox(8, zoomOutButton, zoomSliderControl, zoomInButton, resetZoomButton);
        controls.getStyleClass().add("zoom-controls");
        controls.setAlignment(Pos.CENTER_RIGHT);
        installZoomStepFallback(controls, zoomOutButton, -10, controller);
        installZoomStepFallback(controls, zoomInButton, 10, controller);
        return controls;
    }

    private StackPane zoomSliderControl() {
        Region track = new Region();
        track.getStyleClass().add("zoom-progress-track");
        track.setMouseTransparent(true);
        track.setTranslateX(8);
        track.prefWidthProperty().bind(zoomSlider.widthProperty().subtract(16));
        track.maxWidthProperty().bind(track.prefWidthProperty());
        zoomProgressFill.getStyleClass().add("zoom-progress-fill");
        zoomProgressFill.setMouseTransparent(true);
        zoomProgressFill.setTranslateX(8);
        zoomProgressFill.prefWidthProperty().bind(zoomSlider.widthProperty()
                .subtract(16)
                .multiply(zoomSlider.valueProperty().subtract(zoomSlider.getMin())
                        .divide(zoomSlider.getMax() - zoomSlider.getMin())));
        zoomProgressFill.maxWidthProperty().bind(zoomProgressFill.prefWidthProperty());
        StackPane sliderBox = new StackPane(track, zoomProgressFill, zoomSlider);
        sliderBox.getStyleClass().add("zoom-slider-box");
        sliderBox.setAlignment(Pos.CENTER_LEFT);
        StackPane.setAlignment(track, Pos.CENTER_LEFT);
        StackPane.setAlignment(zoomProgressFill, Pos.CENTER_LEFT);
        return sliderBox;
    }

    private HBox documentStats() {
        Region separator = new Region();
        separator.getStyleClass().add("status-separator");
        HBox stats = new HBox(14, separator, wordCountLabel, topicCountLabel);
        stats.getStyleClass().add("document-stats");
        stats.setAlignment(Pos.CENTER_RIGHT);
        return stats;
    }

    private Button zoomStepButton(String text, String tooltip, double delta, MindMapController controller) {
        Button button = new Button(text);
        button.getStyleClass().add("status-zoom-button");
        button.setFocusTraversable(false);
        button.setPickOnBounds(true);
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(event -> stepZoom(delta, controller));
        return button;
    }

    private void installZoomStepFallback(HBox controls, Button button, double delta, MindMapController controller) {
        controls.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.PRIMARY
                    && button.localToScene(button.getBoundsInLocal()).contains(event.getSceneX(), event.getSceneY())) {
                stepZoom(delta, controller);
                event.consume();
            }
        });
    }

    private void stepZoom(double delta, MindMapController controller) {
        double nextValue = Math.max(zoomSlider.getMin(), Math.min(zoomSlider.getMax(), zoomSlider.getValue() + delta));
        controller.changeZoom(nextValue / 100.0);
    }

    private Button statusButton(String text, String tooltip, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("status-zoom-button");
        button.setFocusTraversable(false);
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
        updatingZoomControl = true;
        zoomSlider.setValue(Math.round(zoom * 100));
        updatingZoomControl = false;
        resetZoomButton.setText(text);
    }

    public void setDocumentStats(int wordCount, int topicCount) {
        wordCountLabel.setText("字数：" + wordCount);
        topicCountLabel.setText("主题数：" + topicCount);
    }
}
