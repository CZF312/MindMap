package com.example.mindmap.util;

import com.example.mindmap.model.ConnectionShape;
import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryUtilsTest {
    @Test
    void geometryChangedDetectsMovementButNotEqualCopies() {
        MindMap before = new MindMap();
        MindMap after = before.deepCopy();

        assertFalse(GeometryUtils.geometryChanged(before, after));

        after.getRoot().moveBy(10, 0);

        assertTrue(GeometryUtils.geometryChanged(before, after));
    }

    @Test
    void nodeIntersectionUsesNodeBounds() {
        MindNode node = new MindNode("n", "node");
        node.setX(20);
        node.setY(30);
        node.setWidth(100);
        node.setHeight(60);

        assertTrue(GeometryUtils.intersectsNode(node, 0, 0, 25, 35));
        assertFalse(GeometryUtils.intersectsNode(node, 0, 0, 10, 20));
    }

    @Test
    void connectionIntersectionSupportsCurveAndElbowShapes() {
        MindNode parent = new MindNode("p", "parent");
        MindNode child = new MindNode("c", "child");
        parent.setX(0);
        parent.setY(0);
        child.setX(300);
        child.setY(0);
        parent.addChild(child);

        assertTrue(GeometryUtils.intersectsConnection(child, 140, 10, 180, 45));

        child.getConnectionStyle().setShape(ConnectionShape.ELBOW);

        assertTrue(GeometryUtils.intersectsConnection(child, 140, 10, 180, 45));
    }
}
