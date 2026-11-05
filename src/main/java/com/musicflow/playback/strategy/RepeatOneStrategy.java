package com.musicflow.playback.strategy;

import com.musicflow.model.Track;

import java.util.List;

public final class RepeatOneStrategy implements PlaybackStrategy {

    @Override
    public int nextIndex(List<Track> tracks, int currentIndex) {
        PlaybackStrategy.validatePosition(tracks, currentIndex);

        if (tracks.isEmpty()) {
            return -1;
        }

        return currentIndex == -1 ? 0 : currentIndex;
    }

    @Override
    public String displayName() {
        return "Repeat one";
    }
}