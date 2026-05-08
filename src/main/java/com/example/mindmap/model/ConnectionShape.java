package com.example.mindmap.model;

public enum ConnectionShape {
    CURVE("曲线"),
    ELBOW("折线");

    private final String label;

    ConnectionShape(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
