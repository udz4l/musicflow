package com.musicflow.playback.strategy;

import com.musicflow.model.Track;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class ShuffleStrategy implements PlaybackStrategy {

    private final RandomGenerator random;

    public ShuffleStrategy() {
        this(RandomGenerator.getDefault());
    }

    public ShuffleStrategy(RandomGenerator random) {
        this.random = Objects.requireNonNull(
                random,
                "Random generator is required"
        );
    }

    @Override
    public int nextIndex(List<Track> tracks, int currentIndex) {
        PlaybackStrategy.validatePosition(tracks, currentIndex);

        int size = tracks.size();

        if (size == 0) {
            return -1;
        }

        if (size == 1) {
            return 0;
        }

        if (currentIndex == -1) {
            return random.nextInt(size);
        }

        int candidate = random.nextInt(size - 1);

        return candidate >= currentIndex ? candidate + 1 : candidate;
    }

    @Override
    public String displayName() {
        return "Shuffle";
    }
}