package com.example.mindmap.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

public class RecentFileService {
    private static final String KEY = "recentFiles";
    private static final int LIMIT = 10;
    private final Preferences preferences = Preferences.userNodeForPackage(RecentFileService.class);

    public List<Path> load() {
        String raw = preferences.get(KEY, "");
        if (raw.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(raw.split("\\|", -1))
                .filter(value -> !value.isBlank())
                .map(Path::of)
                .toList();
    }

    public void add(Path path) {
        if (path == null) {
            return;
        }
        Path normalized = path.toAbsolutePath().normalize();
        List<Path> paths = new ArrayList<>(load());
        paths.removeIf(existing -> existing.toAbsolutePath().normalize().equals(normalized));
        paths.add(0, normalized);
        save(paths.stream().limit(LIMIT).toList());
    }

    public void remove(Path path) {
        if (path == null) {
            return;
        }
        Path normalized = path.toAbsolutePath().normalize();
        save(load().stream().filter(existing -> !existing.toAbsolutePath().normalize().equals(normalized)).toList());
    }

    public boolean exists(Path path) {
        return path != null && Files.exists(path);
    }

    private void save(List<Path> paths) {
        preferences.put(KEY, String.join("|", paths.stream().map(Path::toString).toList()));
    }
}
