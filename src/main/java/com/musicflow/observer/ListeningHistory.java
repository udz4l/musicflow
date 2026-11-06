package com.musicflow.observer;

import com.musicflow.event.PlaybackEvent;
import com.musicflow.event.PlaybackListener;
import com.musicflow.model.Track;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ListeningHistory implements PlaybackListener {

    public record Entry(Track track, Instant completedAt) {
    }

    private final List<Entry> entries = new ArrayList<>();

    @Override
    public synchronized void onPlaybackEvent(PlaybackEvent event) {
        if (event.type() == PlaybackEvent.Type.COMPLETED) {
            entries.add(new Entry(
                    event.track(),
                    event.occurredAt()
            ));
        }
    }

    public synchronized List<Entry> entries() {
        return List.copyOf(entries);
    }
}