package com.musicflow.source;

import com.musicflow.model.Track;

import java.io.IOException;
import java.util.List;

public interface MusicSource {

    List<Track> loadTracks() throws IOException;
}