package com.example.mindmap.service;

import com.example.mindmap.model.LayoutType;
import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MindMapLayoutServiceTest {
    @Test
    void automaticLayoutBalancesLargeTopLevelBranchesAcrossSides() {
        // 构造两个较大的一级分支，验证 AUTO 布局会把它们分到中心主题两侧。
        MindMap map = new MindMap();
        map.setLayoutType(LayoutType.AUTO);
        MindNode root = map.getRoot();
        MindNode largeA = map.createNode("large A");
        MindNode smallA = map.createNode("small A");
        MindNode largeB = map.createNode("large B");
        MindNode smallB = map.createNode("small B");
        root.addChild(largeA);
        root.addChild(smallA);
        root.addChild(largeB);
        root.addChild(smallB);
        for (int i = 0; i < 8; i++) {
            largeA.addChild(map.createNode("large A child " + i));
            largeB.addChild(map.createNode("large B child " + i));
        }

        new MindMapLayoutService().layout(map);

        boolean largeAOnRight = largeA.getCenterX() > root.getCenterX();
        boolean largeBOnRight = largeB.getCenterX() > root.getCenterX();
        assertNotEquals(largeAOnRight, largeBOnRight);
    }

    @Test
    void rightLayoutPlacesTopLevelBranchesOnRightSide() {
        // RIGHT 布局下，一级分支中心点应位于根节点中心点右侧。
        MindMap map = new MindMap();
        map.setLayoutType(LayoutType.RIGHT);
        MindNode child = map.createNode("right child");
        map.getRoot().addChild(child);

        new MindMapLayoutService().layout(map);

        assertTrue(child.getCenterX() > map.getRoot().getCenterX());
    }
}
