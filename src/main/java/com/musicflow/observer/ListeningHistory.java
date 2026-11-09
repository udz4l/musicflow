package com.musicflow.observer;

import com.musicflow.event.PlaybackEvent;
import com.musicflow.event.PlaybackListener;
import com.musicflow.model.Track;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class ListeningHistory implements PlaybackListener {

    public record Entry(Track track, Instant completedAt) {
    }

    private static final System.Logger LOGGER =
            System.getLogger(ListeningHistory.class.getName());

    private final List<Entry> entries = new ArrayList<>();
    private final Path file;

    public ListeningHistory() {
        this.file = null;
    }

    public ListeningHistory(Path file) throws IOException {
        this.file = file.toAbsolutePath().normalize();

        if (Files.notExists(this.file)) {
            return;
        }

        try (var reader = Files.newBufferedReader(
                this.file, StandardCharsets.UTF_8
        )) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                try {
                    entries.add(readEntry(line));
                } catch (RuntimeException exception) {
                    LOGGER.log(
                            System.Logger.Level.WARNING,
                            "Skipped a malformed listening history entry",
                            exception
                    );
                }
            }
        }
    }

    @Override
    public synchronized void onPlaybackEvent(PlaybackEvent event) {
        if (event.type() != PlaybackEvent.Type.COMPLETED) {
            return;
        }

        Entry entry = new Entry(event.track(), event.occurredAt());
        entries.add(entry);

        if (file == null) {
            return;
        }

        try {
            Files.createDirectories(file.getParent());

            Files.writeString(
                    file,
                    "\n" + writeEntry(entry) + "\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            LOGGER.log(
                    System.Logger.Level.WARNING,
                    "Could not save listening history to " + file,
                    exception
            );
        }
    }

    public synchronized List<Entry> entries() {
        return List.copyOf(entries);
    }

    private static String writeEntry(Entry entry) {
        Track track = entry.track();

        return String.join(
                "\t",
                entry.completedAt().toString(),
                encode(track.id()),
                encode(track.title()),
                encode(track.artist()),
                encode(track.audioUri().toString())
        );
    }

    private static Entry readEntry(String line) {
        String[] fields = line.split("\t", -1);

        if (fields.length != 5) {
            throw new IllegalArgumentException(
                    "Expected five history fields"
            );
        }

        Track track = new Track(
                decode(fields[1]),
                decode(fields[2]),
                decode(fields[3]),
                URI.create(decode(fields[4]))
        );

        return new Entry(track, Instant.parse(fields[0]));
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String decode(String value) {
        return new String(
                Base64.getDecoder().decode(value),
                StandardCharsets.UTF_8
        );
    }
}