package com.example.mindmap.view;

import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.NodeStyle;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

final class CanvasBackground {
    private CanvasBackground() {
    }

    static String styleFor(MindMap map) {
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

    static ImageView snapshotImage(MindMap map, double x, double y, double width, double height) {
        if (map == null || map.getCanvasImageUri() == null) {
            return null;
        }
        Image image = new Image(map.getCanvasImageUri(), false);
        if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return null;
        }
        double scale = Math.max(width / image.getWidth(), height / image.getHeight());
        double sourceWidth = width / scale;
        double sourceHeight = height / scale;
        double sourceX = (image.getWidth() - sourceWidth) / 2.0;
        double sourceY = (image.getHeight() - sourceHeight) / 2.0;
        ImageView imageView = new ImageView(image);
        imageView.setViewport(new Rectangle2D(sourceX, sourceY, sourceWidth, sourceHeight));
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setX(x);
        imageView.setY(y);
        imageView.setMouseTransparent(true);
        return imageView;
    }

    private static String cssUrl(String uri) {
        return uri.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
