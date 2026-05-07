package com.example.mindmap.model;

import javafx.scene.paint.Color;

public class NodeStyle {
    private Color fillColor;
    private Color borderColor;
    private Color textColor;

    public NodeStyle() {
        this(Color.web("#FFFFFF"), Color.web("#CBD5E1"), Color.web("#0F172A"));
    }

    public NodeStyle(Color fillColor, Color borderColor, Color textColor) {
        this.fillColor = fillColor;
        this.borderColor = borderColor;
        this.textColor = textColor;
    }

    public NodeStyle copy() {
        return new NodeStyle(fillColor, borderColor, textColor);
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    public Color getTextColor() {
        return textColor;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }

    public static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    public static Color fromHex(String value, Color fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Color.web(value);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
