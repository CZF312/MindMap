package com.example.mindmap.model;

import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

public class NodeStyle {
    private Color fillColor;
    private Color borderColor;
    private Color textColor;
    private String fontFamily = "Microsoft YaHei UI";
    private double fontSize = 13;
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean strikethrough;
    private TextAlignment alignment = TextAlignment.CENTER;

    public NodeStyle() {
        this(Color.web("#FFFFFF"), Color.web("#CBD5E1"), Color.web("#0F172A"));
    }

    public NodeStyle(Color fillColor, Color borderColor, Color textColor) {
        this.fillColor = fillColor;
        this.borderColor = borderColor;
        this.textColor = textColor;
    }

    public NodeStyle copy() {
        NodeStyle copy = new NodeStyle(fillColor, borderColor, textColor);
        copy.fontFamily = fontFamily;
        copy.fontSize = fontSize;
        copy.bold = bold;
        copy.italic = italic;
        copy.underline = underline;
        copy.strikethrough = strikethrough;
        copy.alignment = alignment;
        return copy;
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

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = normalizeFontFamily(fontFamily);
    }

    public static String normalizeFontFamily(String fontFamily) {
        if (fontFamily == null || fontFamily.isBlank()) {
            return "Microsoft YaHei UI";
        }
        return switch (fontFamily.trim()) {
            case "SimSun" -> "宋体";
            case "SimHei" -> "黑体";
            case "KaiTi" -> "楷体";
            default -> fontFamily.trim();
        };
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = Math.max(8, Math.min(72, fontSize));
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean isUnderline() {
        return underline;
    }

    public void setUnderline(boolean underline) {
        this.underline = underline;
    }

    public boolean isStrikethrough() {
        return strikethrough;
    }

    public void setStrikethrough(boolean strikethrough) {
        this.strikethrough = strikethrough;
    }

    public TextAlignment getAlignment() {
        return alignment;
    }

    public void setAlignment(TextAlignment alignment) {
        this.alignment = alignment == null ? TextAlignment.CENTER : alignment;
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
