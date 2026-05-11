package com.example.mindmap.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class SearchState {
    private final List<String> resultIds = new ArrayList<>();
    private int index = -1;

    public void setResults(Collection<String> ids) {
        resultIds.clear();
        if (ids != null) {
            resultIds.addAll(ids);
        }
        index = -1;
    }

    public boolean isEmpty() {
        return resultIds.isEmpty();
    }

    public int size() {
        return resultIds.size();
    }

    public int currentIndex() {
        return index;
    }

    public List<String> ids() {
        return List.copyOf(resultIds);
    }

    public String currentOrFirst() {
        if (resultIds.isEmpty()) {
            return null;
        }
        if (index < 0) {
            index = 0;
        }
        return resultIds.get(index);
    }

    public String navigate(int delta) {
        if (resultIds.isEmpty()) {
            return null;
        }
        if (index < 0) {
            index = delta >= 0 ? 0 : resultIds.size() - 1;
        } else {
            index = Math.floorMod(index + delta, resultIds.size());
        }
        return resultIds.get(index);
    }

    public static String replaceFirstIgnoreCase(String text, String target, String replacement) {
        String source = text == null ? "" : text;
        String normalizedSource = source.toLowerCase(Locale.ROOT);
        String normalizedTarget = target.toLowerCase(Locale.ROOT);
        int matchIndex = normalizedSource.indexOf(normalizedTarget);
        if (matchIndex < 0) {
            return source;
        }
        return source.substring(0, matchIndex)
                + safeReplacement(replacement)
                + source.substring(matchIndex + target.length());
    }

    public static String replaceAllIgnoreCase(String text, String target, String replacement) {
        String source = text == null ? "" : text;
        String normalizedSource = source.toLowerCase(Locale.ROOT);
        String normalizedTarget = target.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        int cursor = 0;
        int matchIndex = normalizedSource.indexOf(normalizedTarget);
        while (matchIndex >= 0) {
            result.append(source, cursor, matchIndex).append(safeReplacement(replacement));
            cursor = matchIndex + target.length();
            matchIndex = normalizedSource.indexOf(normalizedTarget, cursor);
        }
        return result.append(source.substring(cursor)).toString();
    }

    private static String safeReplacement(String replacement) {
        return replacement == null ? "" : replacement;
    }
}
