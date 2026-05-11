package com.example.mindmap.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SearchStateTest {
    @Test
    void firstNavigationStartsAtFirstResult() {
        SearchState state = new SearchState();
        state.setResults(List.of("a", "b", "c"));

        assertEquals("a", state.navigate(1));
        assertEquals(0, state.currentIndex());
        assertEquals("b", state.navigate(1));
    }

    @Test
    void backwardNavigationBeforeAnyPositionStartsAtLastResult() {
        SearchState state = new SearchState();
        state.setResults(List.of("a", "b", "c"));

        assertEquals("c", state.navigate(-1));
        assertEquals(2, state.currentIndex());
    }

    @Test
    void replaceHelpersIgnoreCaseAndHandleNullReplacement() {
        assertEquals("Alpha X beta", SearchState.replaceFirstIgnoreCase("Alpha beta beta", "BETA", "X"));
        assertEquals("Alpha  ", SearchState.replaceAllIgnoreCase("Alpha beta BETA", "beta", null));
    }

    @Test
    void emptyResultsReturnNull() {
        SearchState state = new SearchState();

        assertNull(state.navigate(1));
        assertNull(state.currentOrFirst());
    }
}
