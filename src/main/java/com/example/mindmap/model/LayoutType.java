package com.example.mindmap.model;

public enum LayoutType {
    AUTO("自动布局"),
    LEFT("左侧布局"),
    RIGHT("右侧布局");

    private final String label;

    LayoutType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
