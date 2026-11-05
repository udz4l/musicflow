package com.musicflow.playback.strategy;

import com.musicflow.model.Track;

import java.util.List;

public final class SequentialStrategy implements PlaybackStrategy {

    @Override
    public int nextIndex(List<Track> tracks, int currentIndex) {
        PlaybackStrategy.validatePosition(tracks, currentIndex);

        int next = currentIndex + 1;

        return next < tracks.size() ? next : -1;
    }

    @Override
    public String displayName() {
        return "In order";
    }
}