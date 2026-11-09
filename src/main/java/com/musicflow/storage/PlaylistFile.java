package com.musicflow.storage;

import com.musicflow.model.Playlist;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class PlaylistFile {

    public record SavedPlaylist(
            String name,
            String description,
            List<String> trackIds
    ) {
        public SavedPlaylist {
            trackIds = List.copyOf(trackIds);
        }
    }

    public void save(Path path, Playlist playlist) throws IOException {
        Path destination = path.toAbsolutePath().normalize();

        Properties values = new Properties();
        values.setProperty("format", "1");
        values.setProperty("name", playlist.name());
        values.setProperty("description", playlist.description());
        values.setProperty(
                "track.count",
                Integer.toString(playlist.tracks().size())
        );

        for (int i = 0; i < playlist.tracks().size(); i++) {
            values.setProperty(
                    "track." + i,
                    playlist.tracks().get(i).id()
            );
        }

        Path temporary = Files.createTempFile(
                destination.getParent(),
                "musicflow-",
                ".tmp"
        );

        try {
            try (var writer = Files.newBufferedWriter(
                    temporary, StandardCharsets.UTF_8
            )) {
                values.store(writer, "MusicFlow playlist");
            }

            try {
                Files.move(
                        temporary,
                        destination,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(
                        temporary,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public SavedPlaylist load(Path path) throws IOException {
        Properties values = new Properties();

        try (var reader = Files.newBufferedReader(
                path, StandardCharsets.UTF_8
        )) {
            values.load(reader);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid playlist file", exception);
        }

        if (!"1".equals(values.getProperty("format"))) {
            throw new IOException("Unsupported playlist format");
        }

        String name = required(values, "name");
        String description = values.getProperty("description", "");

        int count;

        try {
            count = Integer.parseInt(required(values, "track.count"));
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid track count", exception);
        }

        if (count < 0 || count > values.size()) {
            throw new IOException("Invalid track count");
        }

        List<String> ids = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            ids.add(required(values, "track." + i));
        }

        return new SavedPlaylist(name, description, ids);
    }

    private String required(Properties values, String key)
            throws IOException {

        String value = values.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IOException("Missing playlist field: " + key);
        }

        return value;
    }
}