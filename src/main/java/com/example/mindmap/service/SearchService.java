package com.example.mindmap.service;

import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;

import java.util.List;
import java.util.Locale;

public class SearchService {
    public List<MindNode> search(MindMap map, String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (map == null || normalized.isEmpty()) {
            return List.of();
        }
        return map.allNodes().stream()
                .filter(node -> node.getText().toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
    }
}
