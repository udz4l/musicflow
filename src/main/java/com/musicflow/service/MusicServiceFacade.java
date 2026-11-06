package com.musicflow.service;

import com.musicflow.audio.PcmAudioPlayer;
import com.musicflow.audio.effect.AudioProcessor;
import com.musicflow.audio.effect.DryProcessor;
import com.musicflow.audio.effect.EchoDecorator;
import com.musicflow.audio.effect.VolumeDecorator;
import com.musicflow.event.PlaybackListener;
import com.musicflow.model.Playlist;
import com.musicflow.model.Track;
import com.musicflow.observer.ListeningHistory;
import com.musicflow.playback.ObservablePlayer;
import com.musicflow.playback.strategy.PlaybackStrategy;
import com.musicflow.playback.strategy.SequentialStrategy;
import com.musicflow.source.MusicSource;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class MusicServiceFacade {

    private final MusicSource source;
    private final ObservablePlayer player;
    private final ListeningHistory history = new ListeningHistory();

    private List<Track> catalog = List.of();
    private Playlist playlist;
    private int currentIndex = -1;

    private PlaybackStrategy strategy = new SequentialStrategy();
    private float volume = 0.7f;
    private boolean echoEnabled;

    public MusicServiceFacade(
            MusicSource source,
            ObservablePlayer player
    ) {
        this.source = Objects.requireNonNull(
                source,
                "Music source is required"
        );

        this.player = Objects.requireNonNull(
                player,
                "Player is required"
        );

        this.player.addListener(history);
    }

    public List<Track> loadCatalog() throws IOException {
        List<Track> loaded = List.copyOf(source.loadTracks());
        Set<String> ids = new HashSet<>();

        for (Track track : loaded) {
            if (!ids.add(track.id())) {
                throw new IOException(
                        "Duplicate catalog track ID: " + track.id()
                );
            }
        }

        catalog = loaded;
        return catalog;
    }

    public List<Track> search(String query) {
        Objects.requireNonNull(query, "Search query is required");

        String text = query.strip().toLowerCase(Locale.ROOT);

        return catalog.stream()
                .filter(track ->
                        track.title().toLowerCase(Locale.ROOT)
                                .contains(text)
                                || track.artist().toLowerCase(Locale.ROOT)
                                .contains(text))
                .toList();
    }

    public Playlist createPlaylist(
            String name,
            String description,
            List<String> trackIds
    ) {
        Objects.requireNonNull(trackIds, "Track IDs are required");

        Playlist.Builder builder = Playlist.builder(name)
                .description(description);

        for (String id : trackIds) {
            builder.addTrack(findTrack(id));
        }

        return builder.build();
    }

    public void selectPlaylist(Playlist playlist) {
        this.playlist = Objects.requireNonNull(
                playlist,
                "Playlist is required"
        );

        currentIndex = -1;
    }

    public void setStrategy(PlaybackStrategy strategy) {
        this.strategy = Objects.requireNonNull(
                strategy,
                "Playback strategy is required"
        );
    }

    public void setVolume(float volume) {
        if (!Float.isFinite(volume) || volume < 0 || volume > 1) {
            throw new IllegalArgumentException(
                    "Volume must be between 0 and 1"
            );
        }

        this.volume = volume;
    }

    public void setEchoEnabled(boolean enabled) {
        echoEnabled = enabled;
    }

    public Optional<Track> playNext()
            throws IOException,
            UnsupportedAudioFileException,
            LineUnavailableException {

        if (playlist == null) {
            throw new IllegalStateException(
                    "Select a playlist before playback"
            );
        }

        List<Track> tracks = playlist.tracks();
        int nextIndex = strategy.nextIndex(tracks, currentIndex);

        if (nextIndex == -1) {
            return Optional.empty();
        }

        if (nextIndex < 0 || nextIndex >= tracks.size()) {
            throw new IllegalStateException(
                    "Playback strategy returned an invalid index"
            );
        }

        Track track = tracks.get(nextIndex);

        player.play(track, createProcessor());

        // Advance only after successful playback.
        currentIndex = nextIndex;

        return Optional.of(track);
    }

    public List<ListeningHistory.Entry> historyEntries() {
        return history.entries();
    }

    public void addPlaybackListener(PlaybackListener listener) {
        player.addListener(listener);
    }

    public void removePlaybackListener(PlaybackListener listener) {
        player.removeListener(listener);
    }

    private Track findTrack(String id) {
        Objects.requireNonNull(id, "Track ID is required");

        return catalog.stream()
                .filter(track -> track.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown track ID: " + id
                ));
    }

    private AudioProcessor createProcessor() {
        AudioProcessor processor = new DryProcessor();

        if (echoEnabled) {
            processor = new EchoDecorator(
                    processor,
                    PcmAudioPlayer.SAMPLE_RATE,
                    250,
                    0.5f
            );
        }

        return new VolumeDecorator(processor, volume);
    }
}