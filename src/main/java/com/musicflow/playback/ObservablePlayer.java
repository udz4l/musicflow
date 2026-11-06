package com.musicflow.playback;

import com.musicflow.audio.PcmAudioPlayer;
import com.musicflow.audio.PlaybackControl;
import com.musicflow.audio.effect.AudioProcessor;
import com.musicflow.event.PlaybackEvent;
import com.musicflow.event.PlaybackListener;
import com.musicflow.model.Track;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ObservablePlayer {

    private static final System.Logger LOGGER =
            System.getLogger(ObservablePlayer.class.getName());

    private final PcmAudioPlayer player;

    private final CopyOnWriteArrayList<PlaybackListener> listeners =
            new CopyOnWriteArrayList<>();

    public ObservablePlayer(PcmAudioPlayer player) {
        this.player = Objects.requireNonNull(
                player,
                "Audio player is required"
        );
    }

    public void addListener(PlaybackListener listener) {
        listeners.addIfAbsent(
                Objects.requireNonNull(listener, "Listener is required")
        );
    }

    public void removeListener(PlaybackListener listener) {
        listeners.remove(listener);
    }

    public void play(Track track, AudioProcessor processor)
            throws IOException,
            UnsupportedAudioFileException,
            LineUnavailableException {

        play(track, processor, new PlaybackControl());
    }

    public boolean play(
            Track track,
            AudioProcessor processor,
            PlaybackControl control
    ) throws IOException,
            UnsupportedAudioFileException,
            LineUnavailableException {

        Objects.requireNonNull(track, "Track is required");
        Objects.requireNonNull(processor, "Processor is required");
        Objects.requireNonNull(control, "Playback control is required");

        publish(
                PlaybackEvent.Type.REQUESTED,
                track,
                "Playback requested"
        );

        try {
            boolean completed = player.play(
                    track.audioUri(),
                    processor,
                    control
            );

            publish(
                    completed
                            ? PlaybackEvent.Type.COMPLETED
                            : PlaybackEvent.Type.STOPPED,
                    track,
                    completed ? "Playback completed" : "Playback stopped"
            );

            return completed;
        } catch (IOException
                 | UnsupportedAudioFileException
                 | LineUnavailableException
                 | RuntimeException exception) {

            String message = exception.getMessage();

            publish(
                    PlaybackEvent.Type.FAILED,
                    track,
                    message == null
                            ? exception.getClass().getSimpleName()
                            : message
            );

            throw exception;
        }
    }

    private void publish(
            PlaybackEvent.Type type,
            Track track,
            String message
    ) {
        PlaybackEvent event = new PlaybackEvent(
                type,
                track,
                Instant.now(),
                message
        );

        for (PlaybackListener listener : listeners) {
            try {
                listener.onPlaybackEvent(event);
            } catch (RuntimeException exception) {
                LOGGER.log(
                        System.Logger.Level.WARNING,
                        "Playback listener failed",
                        exception
                );
            }
        }
    }
}