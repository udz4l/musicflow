package com.musicflow.audio;

import javax.sound.sampled.SourceDataLine;

public final class PlaybackControl {

    private boolean paused;
    private boolean stopped;
    private SourceDataLine line;

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