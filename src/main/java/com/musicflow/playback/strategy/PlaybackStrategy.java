package com.musicflow.playback.strategy;

import com.musicflow.model.Track;

import java.util.List;

public interface PlaybackStrategy {

    int nextIndex(List<Track> tracks, int currentIndex);

    String displayName();

    static void validatePosition(List<Track> tracks, int currentIndex) {
        if (tracks == null) {
            throw new IllegalArgumentException("Track list is required");
        }

        if (currentIndex < -1 || currentIndex >= tracks.size()) {
            throw new IllegalArgumentException(
                    "Invalid current track index: " + currentIndex
            );
        }
    }
}