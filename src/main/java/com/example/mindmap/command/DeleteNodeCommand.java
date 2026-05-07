package com.example.mindmap.command;

import com.example.mindmap.model.MindMap;

public class DeleteNodeCommand extends MindMapSnapshotCommand {
    public DeleteNodeCommand(MindMap target, Runnable change) {
        super(target, change);
    }
}
