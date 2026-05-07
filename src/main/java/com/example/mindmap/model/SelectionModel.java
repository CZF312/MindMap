package com.example.mindmap.model;

import java.util.LinkedHashSet;
import java.util.Set;

public class SelectionModel {
    private final Set<String> selectedNodeIds = new LinkedHashSet<>();
    private String primaryNodeId;

    public void selectOnly(MindNode node) {
        selectedNodeIds.clear();
        if (node != null) {
            selectedNodeIds.add(node.getId());
            primaryNodeId = node.getId();
        } else {
            primaryNodeId = null;
        }
    }

    public void toggle(MindNode node) {
        if (node == null) {
            return;
        }
        if (selectedNodeIds.contains(node.getId())) {
            selectedNodeIds.remove(node.getId());
            if (node.getId().equals(primaryNodeId)) {
                primaryNodeId = selectedNodeIds.stream().reduce((first, second) -> second).orElse(null);
            }
        } else {
            selectedNodeIds.add(node.getId());
            primaryNodeId = node.getId();
        }
    }

    public boolean contains(MindNode node) {
        return node != null && selectedNodeIds.contains(node.getId());
    }

    public Set<String> getSelectedNodeIds() {
        return selectedNodeIds;
    }

    public String getPrimaryNodeId() {
        return primaryNodeId;
    }

    public void setPrimaryNodeId(String primaryNodeId) {
        this.primaryNodeId = primaryNodeId;
        if (primaryNodeId != null) {
            selectedNodeIds.add(primaryNodeId);
        }
    }

    public void clear() {
        selectedNodeIds.clear();
        primaryNodeId = null;
    }
}
