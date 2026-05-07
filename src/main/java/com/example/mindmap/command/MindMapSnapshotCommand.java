package com.example.mindmap.command;

import com.example.mindmap.model.MindMap;

abstract class MindMapSnapshotCommand implements Command {
    private final MindMap target;
    private final Runnable change;
    private final MindMap before;
    private MindMap after;

    MindMapSnapshotCommand(MindMap target, Runnable change) {
        this.target = target;
        this.change = change;
        this.before = target.deepCopy();
    }

    @Override
    public void execute() {
        if (after == null) {
            change.run();
            target.setModified(true);
            after = target.deepCopy();
        } else {
            target.copyFrom(after);
            target.setModified(true);
        }
    }

    @Override
    public void undo() {
        target.copyFrom(before);
        target.setModified(true);
    }
}
