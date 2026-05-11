package com.example.mindmap.service;

import com.example.mindmap.command.Command;

import java.util.ArrayDeque;

public class EditHistory {
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
