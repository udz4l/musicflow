package com.musicflow;

import com.musicflow.audio.PcmAudioPlayer;
import com.musicflow.audio.PlaybackControl;
import com.musicflow.audio.effect.DryProcessor;
import com.musicflow.audio.effect.VolumeDecorator;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public class ControlsDemo {

    public static void main(String[] args) throws Exception {
        PlaybackControl control = new PlaybackControl();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Boolean> completed = new AtomicReference<>();

        Thread worker = new Thread(() -> {
            try {
                boolean result = new PcmAudioPlayer().play(
                        Path.of(
                                "demo-audio",
                                "morning-walk.wav"
                        ).toUri(),
                        new VolumeDecorator(new DryProcessor(), 0.6f),
                        control
                );

                completed.set(result);
            } catch (Throwable exception) {
                failure.set(exception);
            }
        }, "musicflow-controls-demo");

        worker.start();

        try {
            System.out.println("PLAY: sound should start.");
            Thread.sleep(2000);

            control.pause();
            System.out.println("PAUSE: two seconds of silence.");
            Thread.sleep(2000);

            control.resume();
            System.out.println("RESUME: sound should continue.");
            Thread.sleep(2000);

            control.stop();
            System.out.println("STOP: playback should end.");

            worker.join(10000);

            if (worker.isAlive()) {
                throw new IllegalStateException(
                        "Playback did not stop in time"
                );
            }

            if (failure.get() != null) {
                throw new IllegalStateException(
                        "Playback failed",
                        failure.get()
                );
            }

            if (!Boolean.FALSE.equals(completed.get())) {
                throw new IllegalStateException(
                        "Stopped playback must return false"
                );
            }

            System.out.println("All playback control checks passed.");
        } finally {
            control.stop();
            worker.join(10000);
        }
    }
}