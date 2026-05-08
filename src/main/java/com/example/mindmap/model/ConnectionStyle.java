package com.example.mindmap.model;

import javafx.scene.paint.Color;

public class ConnectionStyle {
    private Color color = Color.web("#94A3B8");
    private double width = 2.2;
    private boolean dashed;
    private ConnectionShape shape = ConnectionShape.CURVE;

    public ConnectionStyle copy() {
        ConnectionStyle copy = new ConnectionStyle();
        copy.color = color;
        copy.width = width;
        copy.dashed = dashed;
        copy.shape = shape;
        return copy;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color == null ? Color.web("#94A3B8") : color;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = Math.max(1.0, Math.min(8.0, width));
    }

    public boolean isDashed() {
        return dashed;
    }

    public void setDashed(boolean dashed) {
        this.dashed = dashed;
    }

    public ConnectionShape getShape() {
        return shape;
    }

    public void setShape(ConnectionShape shape) {
        this.shape = shape == null ? ConnectionShape.CURVE : shape;
    }
}
