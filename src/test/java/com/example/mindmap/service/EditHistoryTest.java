package com.example.mindmap.service;

import com.example.mindmap.command.Command;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditHistoryTest {
    @Test
    void executeUndoAndRedoMoveCommandBetweenStacks() {
        AtomicInteger value = new AtomicInteger();
        EditHistory history = new EditHistory();
        Command command = new CounterCommand(value);

        history.execute(command);

        assertEquals(1, value.get());
        assertTrue(history.canUndo());
        assertFalse(history.canRedo());

        assertTrue(history.undo());
        assertEquals(0, value.get());
        assertFalse(history.canUndo());
        assertTrue(history.canRedo());

        assertTrue(history.redo());
        assertEquals(1, value.get());
        assertTrue(history.canUndo());
        assertFalse(history.canRedo());
    }

    @Test
    void newExecutedCommandClearsRedoStack() {
        AtomicInteger value = new AtomicInteger();
        EditHistory history = new EditHistory();

        history.execute(new CounterCommand(value));
        history.undo();
        history.execute(new CounterCommand(value));

        assertFalse(history.canRedo());
        assertEquals(1, value.get());
    }

    private record CounterCommand(AtomicInteger value) implements Command {
        @Override
        public void execute() {
            value.incrementAndGet();
        }

        @Override
        public void undo() {
            value.decrementAndGet();
        }
    }
}
