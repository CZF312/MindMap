package com.example.mindmap.command;

import com.example.mindmap.model.MindMap;

public class ChangeStyleCommand extends MindMapSnapshotCommand {
    public ChangeStyleCommand(MindMap target, Runnable change) {
        super(target, change);
    }
}
