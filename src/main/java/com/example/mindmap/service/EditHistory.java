package com.example.mindmap.service;

import com.example.mindmap.command.Command;

import java.util.ArrayDeque;

public class EditHistory {
    // 使用两个栈分别管理撤销和重做，控制器不需要直接维护历史细节。
    private final ArrayDeque<Command> undoStack = new ArrayDeque<>();
    private final ArrayDeque<Command> redoStack = new ArrayDeque<>();

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    public void execute(Command command) {
        command.execute();
        pushExecuted(command);
    }

    public void pushExecuted(Command command) {
        undoStack.push(command);
        // 新编辑会产生新的历史分支，因此需要清空原有重做栈。
        redoStack.clear();
    }

    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        return true;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
}
