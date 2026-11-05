package com.musicflow.source;

import com.musicflow.model.Track;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class LocalCatalogAdapter implements MusicSource {

    private final LocalCatalogReader reader;
    private final Path catalogFile;

    public LocalCatalogAdapter(
            LocalCatalogReader reader,
            Path catalogFile
    ) {
        this.reader = Objects.requireNonNull(reader, "Reader is required");
        this.catalogFile = Objects.requireNonNull(
                catalogFile,
                "Catalog path is required"
        ).toAbsolutePath().normalize();
    }

    @Override
    public List<Track> loadTracks() throws IOException {
        List<Track> tracks = new ArrayList<>();
        Set<String> ids = new HashSet<>();

        for (LocalCatalogReader.Entry entry
                : reader.readEntries(catalogFile)) {

            if (!ids.add(entry.code())) {
                throw new IOException(
                        "Duplicate track ID: " + entry.code()
                );
            }

            try {
                Path audioFile = catalogFile.getParent()
                        .resolve(entry.fileName())
                        .normalize();

                tracks.add(new Track(
                        entry.code(),
                        entry.songName(),
                        entry.performer(),
                        audioFile.toUri()
                ));
            } catch (InvalidPathException exception) {
                throw new IOException(
                        "Invalid audio path for track: " + entry.code(),
                        exception
                );
            } catch (IllegalArgumentException exception) {
                throw new IOException(
                        "Invalid track: " + entry.code(),
                        exception
                );
            }
        }

        return List.copyOf(tracks);
    }
}