package com.musicflow;

import com.musicflow.model.Playlist;
import com.musicflow.model.Track;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        Track first = new Track(
                "demo-01",
                "Morning Walk",
                "MusicFlow Demo",
                Path.of("demo-audio", "morning-walk.wav").toUri()
        );

        Track second = new Track(
                "demo-02",
                "Night Drive",
                "MusicFlow Demo",
                Path.of("demo-audio", "night-drive.wav").toUri()
        );

        Playlist playlist = Playlist.builder("Daily Mix")
                .description("Our first MusicFlow playlist")
                .addTrack(first)
                .addTrack(second)
                .build();

        System.out.println("MusicFlow");
        System.out.println("Playlist: " + playlist.name());
        System.out.println("Description: " + playlist.description());
        System.out.println("Tracks: " + playlist.tracks().size());

        for (Track track : playlist.tracks()) {
            System.out.println(track.artist() + " - " + track.title());
        }
    }
}