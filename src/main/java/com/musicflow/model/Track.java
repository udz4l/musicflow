package com.musicflow.model;

import java.net.URI;
import java.util.Objects;

public record Track(
        String id,
        String title,
        String artist,
        URI audioUri
) {

    public Track {
        id = requireText(id, "Track ID");
        title = requireText(title, "Title");
        artist = requireText(artist, "Artist");

        Objects.requireNonNull(audioUri, "Audio URI is required");

        String scheme = audioUri.getScheme();

        if (scheme == null
                || !(scheme.equalsIgnoreCase("file")
                || scheme.equalsIgnoreCase("http")
                || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException(
                    "Audio URI must use file, http, or https"
            );
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        return value.strip();
    }
}