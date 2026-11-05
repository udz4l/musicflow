package com.musicflow;

import com.musicflow.model.Playlist;
import com.musicflow.model.Track;
import com.musicflow.source.LocalCatalogAdapter;
import com.musicflow.source.LocalCatalogReader;
import com.musicflow.source.MusicSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class CatalogDemo {

    public static void main(String[] args) throws IOException {
        MusicSource source = new LocalCatalogAdapter(
                new LocalCatalogReader(),
                Path.of("demo-audio", "catalog.txt")
        );

        List<Track> tracks = source.loadTracks();

        Playlist.Builder builder = Playlist.builder("Local discoveries")
                .description("Loaded from the local catalog");

        for (Track track : tracks) {
            builder.addTrack(track);
        }

        Playlist playlist = builder.build();

        System.out.println("Playlist: " + playlist.name());
        System.out.println("Loaded tracks: " + playlist.tracks().size());

        for (Track track : playlist.tracks()) {
            System.out.println(
                    track.id() + " | "
                            + track.artist() + " - "
                            + track.title()
            );
            System.out.println("Audio: " + track.audioUri());
        }
    }
}