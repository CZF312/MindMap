package com.example.mindmap.model;

import com.example.mindmap.command.RenameNodeCommand;
import com.example.mindmap.service.MindMapFileService;
import com.example.mindmap.service.MindMapLayoutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MindMapBehaviorTest {
    @TempDir
    Path tempDir;

    @Test
    void undoRestoresEditableContentWithoutRestoringFilePathOrZoom() {
        MindMap map = new MindMap();
        map.getRoot().setText("before");
        map.setFilePath(Path.of("before.mindmap"));
        map.setZoom(2.0);
        map.setModified(false);

        RenameNodeCommand command = new RenameNodeCommand(map, () -> map.getRoot().setText("after"));
        command.execute();
        map.setFilePath(Path.of("saved-after-command.mindmap"));
        map.setZoom(0.6);
        map.setModified(false);

        command.undo();

        assertEquals("before", map.getRoot().getText());
        assertEquals(Path.of("saved-after-command.mindmap"), map.getFilePath());
        assertEquals(0.6, map.getZoom());
        assertTrue(map.isModified());
    }

    @Test
    void movingSubtreeOnlyRecordsOffsetOnDraggedRoot() {
        MindNode parent = new MindNode("parent", "parent");
        MindNode child = new MindNode("child", "child");
        parent.addChild(child);

        parent.moveBy(12, -5);

        assertEquals(12, parent.getX());
        assertEquals(-5, parent.getY());
        assertEquals(12, parent.getOffsetX());
        assertEquals(-5, parent.getOffsetY());
        assertEquals(12, child.getX());
        assertEquals(-5, child.getY());
        assertEquals(0, child.getOffsetX());
        assertEquals(0, child.getOffsetY());
    }

    @Test
    void automaticLayoutBalancesLargeTopLevelBranchesAcrossSides() {
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
    void loaderUsesOnlyDirectNodeChildAsRoot() throws Exception {
        Path path = tempDir.resolve("direct-root.mindmap");
        Files.writeString(path, """
                <?xml version="1.0" encoding="UTF-8"?>
                <mindmap version="1" name="direct-root" layout="AUTO">
                  <wrapper>
                    <node id="wrong" text="wrong"/>
                  </wrapper>
                  <node id="right" text="right"/>
                </mindmap>
                """);

        MindMap map = new MindMapFileService().load(path);

        assertEquals("right", map.getRoot().getId());
        assertEquals("right", map.getRoot().getText());
    }
}
