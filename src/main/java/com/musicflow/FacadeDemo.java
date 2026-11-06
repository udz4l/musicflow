package com.musicflow;

import com.musicflow.audio.PcmAudioPlayer;
import com.musicflow.model.Playlist;
import com.musicflow.model.Track;
import com.musicflow.playback.ObservablePlayer;
import com.musicflow.playback.strategy.RepeatOneStrategy;
import com.musicflow.playback.strategy.SequentialStrategy;
import com.musicflow.service.MusicServiceFacade;
import com.musicflow.source.LocalCatalogAdapter;
import com.musicflow.source.LocalCatalogReader;

import java.nio.file.Path;
import java.util.List;

public class FacadeDemo {

    public static void main(String[] args) throws Exception {
        MusicServiceFacade music = new MusicServiceFacade(
                new LocalCatalogAdapter(
                        new LocalCatalogReader(),
                        Path.of("demo-audio", "catalog.txt")
                ),
                new ObservablePlayer(new PcmAudioPlayer())
        );

        music.addPlaybackListener(event -> System.out.println(
                event.type() + ": " + event.track().title()
        ));

        List<Track> catalog = music.loadCatalog();

        check("Catalog is loaded", !catalog.isEmpty());

        Track selected = catalog.getFirst();

        check(
                "Search finds the selected track",
                music.search(selected.title()).contains(selected)
        );

        Playlist playlist = music.createPlaylist(
                "Evening session",
                "A short facade demonstration",
                List.of(selected.id())
        );

        music.selectPlaylist(playlist);
        music.setVolume(0.6f);
        music.setEchoEnabled(true);

        check(
                "Sequential mode plays the track",
                music.playNext().isPresent()
        );

        check(
                "Sequential mode reaches the end",
                music.playNext().isEmpty()
        );

        music.setStrategy(new RepeatOneStrategy());
        music.setEchoEnabled(false);

        check(
                "Repeat mode replays the current track",
                music.playNext().isPresent()
        );

        check(
                "Both completed plays are in history",
                music.historyEntries().size() == 2
        );

        music.selectPlaylist(
                music.createPlaylist(
                        "Empty playlist",
                        "",
                        List.of()
                )
        );

        music.setStrategy(new SequentialStrategy());

        check(
                "Empty playlist has no next track",
                music.playNext().isEmpty()
        );

        boolean rejected = false;

        try {
            music.createPlaylist(
                    "Invalid playlist",
                    "",
                    List.of("unknown-track")
            );
        } catch (IllegalArgumentException exception) {
            rejected = true;
        }

        check("Unknown track ID is rejected", rejected);

        System.out.println("All facade checks passed.");
    }

    private static void check(String name, boolean passed) {
        if (!passed) {
            throw new IllegalStateException("FAILED: " + name);
        }

        System.out.println("PASS: " + name);
    }
}