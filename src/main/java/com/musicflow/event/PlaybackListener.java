package com.musicflow.event;

@FunctionalInterface
public interface PlaybackListener {

    void onPlaybackEvent(PlaybackEvent event);
}