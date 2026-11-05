package com.musicflow;

import com.musicflow.model.Track;
import com.musicflow.playback.strategy.PlaybackStrategy;
import com.musicflow.playback.strategy.RepeatOneStrategy;
import com.musicflow.playback.strategy.SequentialStrategy;
import com.musicflow.playback.strategy.ShuffleStrategy;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;

public class StrategyDemo {

    public static void main(String[] args) {
        List<Track> tracks = List.of(
                track("01", "Morning Walk"),
                track("02", "Night Drive"),
                track("03", "Rainy Window")
        );

        PlaybackStrategy strategy = new SequentialStrategy();

        check("Sequential starts with first track",
                strategy.nextIndex(tracks, -1) == 0);
        check("Sequential advances",
                strategy.nextIndex(tracks, 0) == 1);
        check("Sequential stops at the end",
                strategy.nextIndex(tracks, 2) == -1);

        strategy = new RepeatOneStrategy();

        check("Repeat starts with first track",
                strategy.nextIndex(tracks, -1) == 0);
        check("Repeat keeps current track",
                strategy.nextIndex(tracks, 1) == 1);

        strategy = new ShuffleStrategy(new Random(42));

        boolean validShuffle = true;
        int current = 0;

        for (int i = 0; i < 100; i++) {
            int next = strategy.nextIndex(tracks, current);

            if (next < 0 || next >= tracks.size() || next == current) {
                validShuffle = false;
                break;
            }

            current = next;
        }

        check("Shuffle selects valid tracks without immediate repeats",
                validShuffle);

        for (PlaybackStrategy mode : List.of(
                new SequentialStrategy(),
                new RepeatOneStrategy(),
                new ShuffleStrategy()
        )) {
            check(mode.displayName() + ": empty playlist",
                    mode.nextIndex(List.of(), -1) == -1);
        }

        check("Shuffle handles one track",
                strategy.nextIndex(List.of(tracks.getFirst()), 0) == 0);

        boolean rejected = false;

        try {
            strategy.nextIndex(tracks, tracks.size());
        } catch (IllegalArgumentException exception) {
            rejected = true;
        }

        check("Invalid index is rejected", rejected);

        System.out.println("All strategy checks passed.");
    }

    private static Track track(String id, String title) {
        return new Track(
                id,
                title,
                "MusicFlow Demo",
                Path.of("demo-audio", id + ".wav").toUri()
        );
    }

    private static void check(String name, boolean passed) {
        if (!passed) {
            throw new IllegalStateException("FAILED: " + name);
        }

        System.out.println("PASS: " + name);
    }
}