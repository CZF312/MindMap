package com.example.mindmap.command;

import com.example.mindmap.model.MindMap;

public class RenameNodeCommand extends MindMapSnapshotCommand {
    public RenameNodeCommand(MindMap target, Runnable change) {
        super(target, change);
    }
}
