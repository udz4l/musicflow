package com.musicflow;

import com.musicflow.audio.PcmAudioPlayer;
import com.musicflow.model.Playlist;
import com.musicflow.model.Track;
import com.musicflow.playback.ObservablePlayer;
import com.musicflow.server.DemoMusicServer;
import com.musicflow.service.MusicServiceFacade;
import com.musicflow.source.HttpCatalogAdapter;
import com.musicflow.source.HttpCatalogClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public class HttpPlaybackDemo {

    public static void main(String[] args) throws Exception {
        try (DemoMusicServer server = new DemoMusicServer(
                Path.of("demo-audio")
        )) {
            server.start();

            URI catalogUri = server.catalogUri();

            System.out.println("Server: " + catalogUri);

            MusicServiceFacade music = new MusicServiceFacade(
                    new HttpCatalogAdapter(
                            new HttpCatalogClient(),
                            catalogUri
                    ),
                    new ObservablePlayer(new PcmAudioPlayer())
            );

            music.addPlaybackListener(event -> System.out.println(
                    event.type() + ": " + event.track().title()
            ));

            List<Track> tracks = music.loadCatalog();

            check("HTTP catalog contains tracks", !tracks.isEmpty());

            Track track = tracks.getFirst();

            check(
                    "Track points to the HTTP server",
                    "http".equals(track.audioUri().getScheme())
                            && track.audioUri().getPort()
                            == catalogUri.getPort()
            );

            Playlist playlist = music.createPlaylist(
                    "Network session",
                    "Music received from the local HTTP server",
                    List.of(track.id())
            );

            music.selectPlaylist(playlist);
            music.setVolume(0.6f);
            music.setEchoEnabled(true);

            System.out.println("Streaming: " + track.audioUri());

            check(
                    "HTTP audio playback completes",
                    music.playNext().isPresent()
            );

            check(
                    "Completed HTTP playback is recorded",
                    music.historyEntries().size() == 1
            );

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest missingFile = HttpRequest.newBuilder(
                            catalogUri.resolve("missing.wav")
                    )
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<Void> response = client.send(
                    missingFile,
                    HttpResponse.BodyHandlers.discarding()
            );

            check(
                    "Missing audio returns HTTP 404",
                    response.statusCode() == 404
            );
        }

        System.out.println("All HTTP playback checks passed.");
    }

    private static void check(String name, boolean passed) {
        if (!passed) {
            throw new IllegalStateException("FAILED: " + name);
        }

        System.out.println("PASS: " + name);
    }
}