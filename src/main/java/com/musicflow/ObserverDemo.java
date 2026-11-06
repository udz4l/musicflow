package com.musicflow;

import com.musicflow.audio.PcmAudioPlayer;
import com.musicflow.audio.effect.DryProcessor;
import com.musicflow.audio.effect.VolumeDecorator;
import com.musicflow.event.PlaybackListener;
import com.musicflow.model.Track;
import com.musicflow.observer.ListeningHistory;
import com.musicflow.playback.ObservablePlayer;
import com.musicflow.source.LocalCatalogAdapter;
import com.musicflow.source.LocalCatalogReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ObserverDemo {

    public static void main(String[] args) throws Exception {
        List<Track> tracks = new LocalCatalogAdapter(
                new LocalCatalogReader(),
                Path.of("demo-audio", "catalog.txt")
        ).loadTracks();

        if (tracks.isEmpty()) {
            throw new IllegalStateException(
                    "The demo requires at least one track"
            );
        }

        ObservablePlayer player = new ObservablePlayer(
                new PcmAudioPlayer()
        );

        ListeningHistory history = new ListeningHistory();

        PlaybackListener console = event -> System.out.println(
                event.type() + ": " + event.track().title()
                        + " | " + event.message()
        );

        player.addListener(console);
        player.addListener(history);

        // Registering the same listener twice must not duplicate entries.
        player.addListener(history);

        player.play(
                tracks.getFirst(),
                new VolumeDecorator(new DryProcessor(), 0.7f)
        );

        check(
                "Completed playback is recorded once",
                history.entries().size() == 1
        );

        // Create a guaranteed missing file path without creating the file.
        Path missingDirectory = java.nio.file.Files.createTempDirectory(
                "musicflow-missing-"
        );

        try {
            Track missing = new Track(
                    "missing-track",
                    "Unavailable track",
                    "MusicFlow Demo",
                    missingDirectory.resolve("missing.wav").toUri()
            );

            boolean failed = false;

            try {
                player.play(missing, new DryProcessor());
            } catch (IOException exception) {
                failed = true;
                System.out.println("Expected file error received.");
            }

            check("Missing file is rejected", failed);
            check(
                    "Failed playback is not recorded",
                    history.entries().size() == 1
            );
        } finally {
            java.nio.file.Files.deleteIfExists(missingDirectory);
        }

        player.removeListener(console);
        player.removeListener(history);

        System.out.println("All observer checks passed.");
    }

    private static void check(String name, boolean passed) {
        if (!passed) {
            throw new IllegalStateException("FAILED: " + name);
        }

        System.out.println("PASS: " + name);
    }
}