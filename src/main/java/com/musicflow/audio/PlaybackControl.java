package com.musicflow.audio;

import javax.sound.sampled.SourceDataLine;

public final class PlaybackControl {

    private boolean paused;
    private boolean stopped;
    private SourceDataLine line;

    private long durationFrames = -1;

    public record Progress(long positionMillis, long durationMillis) {
    }

    synchronized void setDurationFrames(long durationFrames) {
        this.durationFrames = durationFrames;
    }

    public synchronized Progress progress() {
        long playedFrames = line != null && line.isOpen()
                ? Math.max(0, line.getLongFramePosition())
                : 0;

        long durationMillis = durationFrames > 0
                ? toMillis(durationFrames)
                : -1;

        long positionMillis = toMillis(playedFrames);

        if (durationMillis >= 0) {
            positionMillis = Math.min(positionMillis, durationMillis);
        }

        return new Progress(positionMillis, durationMillis);
    }

    private static long toMillis(long frames) {
        return (long) (frames * 1000.0 / PcmAudioPlayer.SAMPLE_RATE);
    }

    public synchronized void pause() {
        if (stopped) {
            return;
        }

        paused = true;

        if (line != null && line.isOpen()) {
            line.stop();
        }
    }

    public synchronized void resume() {
        if (stopped) {
            return;
        }

        paused = false;

        if (line != null && line.isOpen()) {
            line.start();
        }

        notifyAll();
    }

    public synchronized void stop() {
        stopped = true;
        paused = false;
        notifyAll();

        if (line != null && line.isOpen()) {
            line.stop();
            line.flush();
            line.close();
        }
    }

    public synchronized boolean isStopped() {
        return stopped;
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    synchronized boolean attach(SourceDataLine output) {
        if (stopped) {
            return false;
        }

        line = output;

        if (!paused) {
            line.start();
        }

        return true;
    }

    synchronized void detach() {
        line = null;
    }

    synchronized boolean awaitReady() {
        while (paused && !stopped) {
            try {
                wait();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                stop();
                return false;
            }
        }

        return !stopped;
    }
}