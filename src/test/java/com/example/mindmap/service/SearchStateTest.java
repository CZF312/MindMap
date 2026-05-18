package com.example.mindmap.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SearchStateTest {
    @Test
    void firstNavigationStartsAtFirstResult() {
        // 首次向后导航时，应定位到第一个搜索结果。
        SearchState state = new SearchState();
        state.setResults(List.of("a", "b", "c"));

        assertEquals("a", state.navigate(1));
        assertEquals(0, state.currentIndex());
        assertEquals("b", state.navigate(1));
    }

    @Test
    void backwardNavigationBeforeAnyPositionStartsAtLastResult() {
        // 首次向前导航时，应从最后一个搜索结果开始。
        SearchState state = new SearchState();
        state.setResults(List.of("a", "b", "c"));

        assertEquals("c", state.navigate(-1));
        assertEquals(2, state.currentIndex());
    }

    @Test
    void replaceHelpersIgnoreCaseAndHandleNullReplacement() {
        // 替换逻辑忽略大小写，replacement 为 null 时按空字符串处理。
        assertEquals("Alpha X beta", SearchState.replaceFirstIgnoreCase("Alpha beta beta", "BETA", "X"));
        assertEquals("Alpha  ", SearchState.replaceAllIgnoreCase("Alpha beta BETA", "beta", null));
    }

    @Test
    void emptyResultsReturnNull() {
        // 没有搜索结果时，导航和获取当前结果都应返回 null。
        SearchState state = new SearchState();

        assertNull(state.navigate(1));
        assertNull(state.currentOrFirst());
    }
}
