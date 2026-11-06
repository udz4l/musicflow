package com.musicflow.tools;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DemoAudioGenerator {

    private static final int SAMPLE_RATE = 44100;
    private static final double NOTE_SECONDS = 0.4;

    public static void main(String[] args) throws IOException {
        Path directory = Path.of("demo-audio");
        Files.createDirectories(directory);

        writeTrack(directory.resolve("morning-walk.wav"),
                new int[]{60, 64, 67, 72, 67, 64, 62, 67});

        writeTrack(directory.resolve("night-drive.wav"),
                new int[]{57, 60, 64, 67, 64, 60, 55, 59});

        writeTrack(directory.resolve("rainy-window.wav"),
                new int[]{62, 65, 69, 72, 69, 65, 60, 64});

        System.out.println("Generated three WAV files in:");
        System.out.println(directory.toAbsolutePath());
    }

    private static void writeTrack(Path file, int[] melody)
            throws IOException {

        int samplesPerNote = (int) (SAMPLE_RATE * NOTE_SECONDS);
        int totalNotes = melody.length * 4;
        int totalSamples = samplesPerNote * totalNotes;

        byte[] pcm = new byte[totalSamples * 2];

        for (int i = 0; i < totalSamples; i++) {
            int noteIndex = i / samplesPerNote;
            int noteSample = i % samplesPerNote;

            double time = noteSample / (double) SAMPLE_RATE;
            double frequency = 440.0 * Math.pow(
                    2.0,
                    (melody[noteIndex % melody.length] - 69) / 12.0
            );

            double attack = Math.min(1.0, time / 0.02);
            double release = Math.min(
                    1.0,
                    (NOTE_SECONDS - time) / 0.08
            );

            double tone = Math.sin(2 * Math.PI * frequency * time)
                    + 0.25 * Math.sin(4 * Math.PI * frequency * time);

            double sample = 0.22 * attack * release * tone;
            short value = (short) Math.round(sample * 32767);

            pcm[i * 2] = (byte) (value & 0xff);
            pcm[i * 2 + 1] = (byte) ((value >>> 8) & 0xff);
        }

        AudioFormat format = new AudioFormat(
                SAMPLE_RATE,
                16,
                1,
                true,
                false
        );

        try (AudioInputStream stream = new AudioInputStream(
                new ByteArrayInputStream(pcm),
                format,
                totalSamples
        )) {
            AudioSystem.write(
                    stream,
                    AudioFileFormat.Type.WAVE,
                    file.toFile()
            );
        }

        System.out.println("Created: " + file.getFileName());
    }
}