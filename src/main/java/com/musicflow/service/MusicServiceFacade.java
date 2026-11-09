package com.musicflow.service;

import com.musicflow.audio.PcmAudioPlayer;
import com.musicflow.audio.PlaybackControl;
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
    private final ListeningHistory history;

    private List<Track> catalog = List.of();
    private Playlist playlist;
    private int currentIndex = -1;

    private volatile PlaybackStrategy strategy = new SequentialStrategy();
    private volatile float volume = 0.7f;
    private volatile boolean echoEnabled;
    private volatile VolumeDecorator activeVolume;

    public MusicServiceFacade(
            MusicSource source,
            ObservablePlayer player
    ) {
        this(source, player, new ListeningHistory());
    }

    public MusicServiceFacade(
            MusicSource source,
            ObservablePlayer player,
            ListeningHistory history
    ) {
        this.source = Objects.requireNonNull(
                source,
                "Music source is required"
        );

        this.player = Objects.requireNonNull(
                player,
                "Player is required"
        );

        this.history = Objects.requireNonNull(
                history,
                "History is required"
        );

        this.player.addListener(this.history);
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
            Track track = catalog.stream()
                    .filter(item -> item.id().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown track ID: " + id
                    ));

            builder.addTrack(track);
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

        VolumeDecorator current = activeVolume;

        if (current != null) {
            current.setGain(volume);
        }
    }

    public void setEchoEnabled(boolean enabled) {
        echoEnabled = enabled;
    }

    public Optional<Track> playNext()
            throws IOException,
            UnsupportedAudioFileException,
            LineUnavailableException {

        return playNext(new PlaybackControl());
    }

    public Optional<Track> playNext(PlaybackControl control)
            throws IOException,
            UnsupportedAudioFileException,
            LineUnavailableException {

        Objects.requireNonNull(control, "Playback control is required");
        requirePlaylist();

        if (control.isStopped()) {
            return Optional.empty();
        }

        int nextIndex = strategy.nextIndex(
                playlist.tracks(),
                currentIndex
        );

        if (nextIndex == -1) {
            return Optional.empty();
        }

        return playAt(nextIndex, control);
    }

    public Optional<Track> playAt(
            int index,
            PlaybackControl control
    ) throws IOException,
            UnsupportedAudioFileException,
            LineUnavailableException {

        Objects.requireNonNull(control, "Playback control is required");
        requirePlaylist();

        if (index < 0 || index >= playlist.tracks().size()) {
            throw new IllegalArgumentException(
                    "Invalid playlist index: " + index
            );
        }

        if (control.isStopped()) {
            return Optional.empty();
        }

        currentIndex = index;
        Track track = playlist.tracks().get(index);

        AudioProcessor processor = new DryProcessor();

        if (echoEnabled) {
            processor = new EchoDecorator(
                    processor,
                    PcmAudioPlayer.SAMPLE_RATE,
                    250,
                    0.5f
            );
        }

        VolumeDecorator output = new VolumeDecorator(
                processor,
                volume
        );

        activeVolume = output;

        // Apply the latest value if it changed during construction.
        output.setGain(volume);

        try {
            boolean completed = player.play(track, output, control);

            return completed
                    ? Optional.of(track)
                    : Optional.empty();
        } finally {
            activeVolume = null;
        }
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

    private void requirePlaylist() {
        if (playlist == null) {
            throw new IllegalStateException(
                    "Select a playlist before playback"
            );
        }
    }
}