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
        // 执行编辑前先保存快照，撤销时可以完整恢复旧状态。
        this.before = target.deepCopy();
    }

    @Override
    public void execute() {
        if (after == null) {
            change.run();
            target.setModified(true);
            // 第一次执行后保存新状态，重做时可直接恢复该快照。
            after = target.deepCopy();
        } else {
            target.copyEditableContentFrom(after);
            target.setModified(true);
        }
    }

    @Override
    public void undo() {
        target.copyEditableContentFrom(before);
        target.setModified(true);
    }
}
