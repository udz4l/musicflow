package com.musicflow.event;

import com.musicflow.model.Track;

import java.time.Instant;
import java.util.Objects;

public record PlaybackEvent(
        Type type,
        Track track,
        Instant occurredAt,
        String message
) {

    public enum Type {
        REQUESTED,
        COMPLETED,
        STOPPED,
        FAILED
    }

    public PlaybackEvent {
        Objects.requireNonNull(type, "Event type is required");
        Objects.requireNonNull(track, "Track is required");
        Objects.requireNonNull(occurredAt, "Event time is required");
        Objects.requireNonNull(message, "Message is required");
    }
}