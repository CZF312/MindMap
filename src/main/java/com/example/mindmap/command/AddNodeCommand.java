package com.example.mindmap.command;

import com.example.mindmap.model.MindMap;

public class AddNodeCommand extends MindMapSnapshotCommand {
    public AddNodeCommand(MindMap target, Runnable change) {
        super(target, change);
    }
}
