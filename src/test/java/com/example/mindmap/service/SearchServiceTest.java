package com.example.mindmap.service;

import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchServiceTest {
    @Test
    void searchFindsMatchingNodesIgnoringCase() {
        // 构造包含大小写差异的节点文本，验证搜索时不区分大小写。
        MindMap map = new MindMap();
        MindNode first = map.createNode("Search Test Alpha");
        MindNode second = map.createNode("search test Beta");
        MindNode other = map.createNode("layout node");
        map.getRoot().addChild(first);
        map.getRoot().addChild(second);
        map.getRoot().addChild(other);

        List<MindNode> results = new SearchService().search(map, "SEARCH TEST");

        assertEquals(2, results.size());
        assertTrue(results.contains(first));
        assertTrue(results.contains(second));
    }

    @Test
    void blankKeywordReturnsEmptyResult() {
        // 空关键词没有明确搜索目标，应直接返回空结果。
        MindMap map = new MindMap();
        map.getRoot().addChild(map.createNode("node"));

        assertTrue(new SearchService().search(map, "   ").isEmpty());
    }
}
