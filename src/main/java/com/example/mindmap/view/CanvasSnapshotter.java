package com.example.mindmap.view;

import com.example.mindmap.model.MindMap;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;

import java.util.ArrayList;
import java.util.List;

final class CanvasSnapshotter {
    private static final double PADDING = 48;

    private CanvasSnapshotter() {
    }

    static WritableImage snapshotFull(Group contentGroup, MindMap map, double scale) {
        List<Transform> oldTransforms = new ArrayList<>(contentGroup.getTransforms());
        contentGroup.getTransforms().clear();
        try {
            return snapshotFullAtScale(contentGroup, map, scale);
        } finally {
            contentGroup.getTransforms().setAll(oldTransforms);
        }
    }

    private static WritableImage snapshotFullAtScale(Group contentGroup, MindMap map, double scale) {
        Bounds contentBounds = contentGroup.getLayoutBounds();
        double minX = contentBounds.getMinX() - PADDING;
        double minY = contentBounds.getMinY() - PADDING;
        double width = Math.max(1, contentBounds.getMaxX() - contentBounds.getMinX() + PADDING * 2);
        double height = Math.max(1, contentBounds.getMaxY() - contentBounds.getMinY() + PADDING * 2);
        double safeScale = Math.max(1.0, scale);
        Rectangle background = new Rectangle(minX, minY, width, height);
        background.setFill(map == null ? Color.WHITE : map.getCanvasColor());
        background.setMouseTransparent(true);
        ImageView backgroundImage = CanvasBackground.snapshotImage(map, minX, minY, width, height);
        contentGroup.getChildren().add(0, background);
        if (backgroundImage != null) {
            contentGroup.getChildren().add(1, backgroundImage);
        }
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setTransform(new Scale(safeScale, safeScale)
                .createConcatenation(Transform.translate(-minX, -minY)));
        try {
            return contentGroup.snapshot(params, new WritableImage(
                    (int) Math.ceil(width * safeScale),
                    (int) Math.ceil(height * safeScale)));
        } finally {
            contentGroup.getChildren().remove(background);
            if (backgroundImage != null) {
                contentGroup.getChildren().remove(backgroundImage);
            }
        }
    }
}
