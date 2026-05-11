package com.example.mindmap.model;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class SelectionModel {
    private final Set<String> selectedNodeIds = new LinkedHashSet<>();
    private final Set<String> selectedConnectionIds = new LinkedHashSet<>();
    private String primaryNodeId;
    private String primaryConnectionId;

    public void selectOnly(MindNode node) {
        selectedNodeIds.clear();
        selectedConnectionIds.clear();
        primaryConnectionId = null;
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
        selectedConnectionIds.clear();
        primaryConnectionId = null;
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

    public void selectAll(Collection<MindNode> nodes) {
        selectedNodeIds.clear();
        selectedConnectionIds.clear();
        primaryNodeId = null;
        primaryConnectionId = null;
        if (nodes == null) {
            return;
        }
        for (MindNode node : nodes) {
            if (node != null) {
                selectedNodeIds.add(node.getId());
                primaryNodeId = node.getId();
            }
        }
    }

    public void selectAll(Collection<MindNode> nodes, Collection<String> connectionIds) {
        selectedNodeIds.clear();
        selectedConnectionIds.clear();
        primaryNodeId = null;
        primaryConnectionId = null;
        if (nodes != null) {
            for (MindNode node : nodes) {
                if (node != null) {
                    selectedNodeIds.add(node.getId());
                    primaryNodeId = node.getId();
                }
            }
        }
        if (connectionIds != null) {
            for (String connectionId : connectionIds) {
                if (connectionId != null) {
                    selectedConnectionIds.add(connectionId);
                    primaryConnectionId = connectionId;
                }
            }
        }
    }

    public boolean contains(MindNode node) {
        return node != null && selectedNodeIds.contains(node.getId());
    }

    public void selectOnlyConnection(String connectionId) {
        selectedNodeIds.clear();
        primaryNodeId = null;
        selectedConnectionIds.clear();
        if (connectionId != null) {
            selectedConnectionIds.add(connectionId);
            primaryConnectionId = connectionId;
        } else {
            primaryConnectionId = null;
        }
    }

    public void toggleConnection(String connectionId) {
        if (connectionId == null) {
            return;
        }
        selectedNodeIds.clear();
        primaryNodeId = null;
        if (selectedConnectionIds.contains(connectionId)) {
            selectedConnectionIds.remove(connectionId);
            if (connectionId.equals(primaryConnectionId)) {
                primaryConnectionId = selectedConnectionIds.stream().reduce((first, second) -> second).orElse(null);
            }
        } else {
            selectedConnectionIds.add(connectionId);
            primaryConnectionId = connectionId;
        }
    }

    public boolean containsConnection(String connectionId) {
        return connectionId != null && selectedConnectionIds.contains(connectionId);
    }

    public Set<String> getSelectedNodeIds() {
        return selectedNodeIds;
    }

    public Set<String> getSelectedConnectionIds() {
        return selectedConnectionIds;
    }

    public String getPrimaryNodeId() {
        return primaryNodeId;
    }

    public String getPrimaryConnectionId() {
        return primaryConnectionId;
    }

    public void setPrimaryNodeId(String primaryNodeId) {
        selectedConnectionIds.clear();
        primaryConnectionId = null;
        this.primaryNodeId = primaryNodeId;
        if (primaryNodeId != null) {
            selectedNodeIds.add(primaryNodeId);
        }
    }

    public void clear() {
        selectedNodeIds.clear();
        selectedConnectionIds.clear();
        primaryNodeId = null;
        primaryConnectionId = null;
    }
}
