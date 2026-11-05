package com.musicflow.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Playlist {

    private final String name;
    private final String description;
    private final List<Track> tracks;

    private Playlist(Builder builder) {
        name = builder.name;
        description = builder.description;
        tracks = List.copyOf(builder.tracks);
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public List<Track> tracks() {
        return tracks;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {

        private final String name;
        private String description = "";
        private final List<Track> tracks = new ArrayList<>();

        private Builder(String name) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "Playlist name must not be blank"
                );
            }

            this.name = name.strip();
        }

        public Builder description(String description) {
            this.description = Objects.requireNonNull(
                    description,
                    "Description is required"
            ).strip();

            return this;
        }

        public Builder addTrack(Track track) {
            Objects.requireNonNull(track, "Track is required");

            boolean duplicate = tracks.stream()
                    .anyMatch(existing -> existing.id().equals(track.id()));

            if (duplicate) {
                throw new IllegalArgumentException(
                        "Track is already in the playlist: " + track.id()
                );
            }

            tracks.add(track);
            return this;
        }

        public Playlist build() {
            return new Playlist(this);
        }
    }
}