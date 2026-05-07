package com.example.mindmap.command;

import com.example.mindmap.model.MindMap;

public class ToggleCollapseCommand extends MindMapSnapshotCommand {
    public ToggleCollapseCommand(MindMap target, Runnable change) {
        super(target, change);
    }
}
