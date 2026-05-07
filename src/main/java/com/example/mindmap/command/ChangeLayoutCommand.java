package com.example.mindmap.command;

import com.example.mindmap.model.MindMap;

public class ChangeLayoutCommand extends MindMapSnapshotCommand {
    public ChangeLayoutCommand(MindMap target, Runnable change) {
        super(target, change);
    }
}
