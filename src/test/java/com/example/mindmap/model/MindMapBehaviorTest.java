package com.example.mindmap.model;

import com.example.mindmap.command.RenameNodeCommand;
import com.example.mindmap.service.MindMapFileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MindMapBehaviorTest {
    @TempDir
    Path tempDir;

    @Test
    void undoRestoresEditableContentWithoutRestoringFilePathOrZoom() {
        // 构造一个已保存过的导图，用于验证撤销只恢复可编辑内容。
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

        // 撤销后应恢复节点文本，但不应回滚当前文件路径和缩放比例。
        command.undo();

        assertEquals("before", map.getRoot().getText());
        assertEquals(Path.of("saved-after-command.mindmap"), map.getFilePath());
        assertEquals(0.6, map.getZoom());
        assertTrue(map.isModified());
    }

    @Test
    void movingSubtreeOnlyRecordsOffsetOnDraggedRoot() {
        // 父节点拖拽时，子节点跟随移动，但只有被拖拽的父节点记录偏移量。
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
    void loaderUsesOnlyDirectNodeChildAsRoot() throws Exception {
        // wrapper 内的 node 不是 mindmap 的直接子节点，不能被误认为中心主题。
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

    @Test
    void fileRoundTripPreservesManualNodeGeometry() throws Exception {
        // 保存再读取后，手动设置的坐标和拖拽偏移量应保持一致。
        MindMap map = new MindMap();
        MindNode root = map.getRoot();
        root.setX(120);
        root.setY(240);
        root.setOffsetX(-30);
        root.setOffsetY(45);
        MindNode child = map.createNode("child");
        child.setX(500);
        child.setY(180);
        child.setOffsetX(70);
        child.setOffsetY(-25);
        root.addChild(child);
        Path path = tempDir.resolve("geometry.mindmap");

        MindMapFileService fileService = new MindMapFileService();
        fileService.save(map, path);
        MindMap loaded = fileService.load(path);
        MindNode loadedRoot = loaded.getRoot();
        MindNode loadedChild = loadedRoot.getChildren().get(0);

        assertEquals(120, loadedRoot.getX());
        assertEquals(240, loadedRoot.getY());
        assertEquals(-30, loadedRoot.getOffsetX());
        assertEquals(45, loadedRoot.getOffsetY());
        assertEquals(500, loadedChild.getX());
        assertEquals(180, loadedChild.getY());
        assertEquals(70, loadedChild.getOffsetX());
        assertEquals(-25, loadedChild.getOffsetY());
    }
}
