package com.musicflow;

import com.musicflow.audio.PcmAudioPlayer;
import com.musicflow.audio.effect.AudioProcessor;
import com.musicflow.audio.effect.DryProcessor;
import com.musicflow.audio.effect.EchoDecorator;
import com.musicflow.audio.effect.VolumeDecorator;
import com.musicflow.model.Track;
import com.musicflow.source.LocalCatalogAdapter;
import com.musicflow.source.LocalCatalogReader;
import com.musicflow.source.MusicSource;

import java.nio.file.Path;
import java.util.List;

public class PlaybackDemo {

    public static void main(String[] args) throws Exception {
        MusicSource source = new LocalCatalogAdapter(
                new LocalCatalogReader(),
                Path.of("demo-audio", "catalog.txt")
        );

        List<Track> tracks = source.loadTracks();

        if (tracks.isEmpty()) {
            System.out.println("The catalog is empty.");
            return;
        }

        Track track = tracks.getFirst();
        PcmAudioPlayer player = new PcmAudioPlayer();

        System.out.println("Playing without effects:");
        System.out.println(track.artist() + " - " + track.title());

        player.play(
                track.audioUri(),
                new DryProcessor()
        );

        System.out.println("Playing with echo and lower volume:");

        AudioProcessor effects = new VolumeDecorator(
                new EchoDecorator(
                        new DryProcessor(),
                        PcmAudioPlayer.SAMPLE_RATE,
                        250,
                        0.5f
                ),
                0.7f
        );

        player.play(track.audioUri(), effects);

        System.out.println("Playback demo completed.");
    }
}