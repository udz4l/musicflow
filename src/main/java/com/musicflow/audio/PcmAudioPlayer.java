package com.musicflow.audio;

import com.musicflow.audio.effect.AudioProcessor;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class PcmAudioPlayer {

    public static final int SAMPLE_RATE = 44100;

    private static final int BLOCK_SAMPLES = 1024;
    private static final int BYTES_PER_SAMPLE = 2;

    public void play(URI audioUri, AudioProcessor processor)
            throws IOException,
            UnsupportedAudioFileException,
            LineUnavailableException {

        Objects.requireNonNull(audioUri, "Audio URI is required");
        Objects.requireNonNull(processor, "Processor is required");

        try (InputStream input = openInput(audioUri);
             BufferedInputStream buffered = new BufferedInputStream(input);
             AudioInputStream audio =
                     AudioSystem.getAudioInputStream(buffered)) {

            AudioFormat format = audio.getFormat();
            validateFormat(format);

            SourceDataLine line = AudioSystem.getSourceDataLine(format);

            try {
                line.open(format);
                line.start();

                byte[] bytes = new byte[
                        BLOCK_SAMPLES * BYTES_PER_SAMPLE
                        ];

                float[] samples = new float[BLOCK_SAMPLES];

                int bytesRead;

                while ((bytesRead = audio.read(
                        bytes,
                        0,
                        bytes.length
                )) != -1) {
                    if (bytesRead == 0) {
                        continue;
                    }

                    int sampleCount = bytesRead / BYTES_PER_SAMPLE;

                    decode(bytes, samples, sampleCount);
                    processor.process(samples, sampleCount);
                    encode(samples, bytes, sampleCount);
                    writeFully(line, bytes, bytesRead);
                }

                // Continue with silence so delayed audio can finish.
                int tailSamples = SAMPLE_RATE * 2;

                while (tailSamples > 0) {
                    int count = Math.min(
                            BLOCK_SAMPLES,
                            tailSamples
                    );

                    java.util.Arrays.fill(samples, 0, count, 0.0f);

                    processor.process(samples, count);
                    encode(samples, bytes, count);
                    writeFully(
                            line,
                            bytes,
                            count * BYTES_PER_SAMPLE
                    );

                    tailSamples -= count;
                }

                line.drain();
            } finally {
                line.stop();
                line.close();
            }
        }
    }

    private InputStream openInput(URI uri) throws IOException {
        String scheme = uri.getScheme();

        if ("file".equalsIgnoreCase(scheme)) {
            return Files.newInputStream(Path.of(uri));
        }

        if ("http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme)) {

            URLConnection connection = uri.toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            return connection.getInputStream();
        }

        throw new IOException("Unsupported audio URI: " + uri);
    }

    private void validateFormat(AudioFormat format)
            throws IOException {

        if (!AudioFormat.Encoding.PCM_SIGNED.equals(
                format.getEncoding()
        )
                || format.getSampleRate() != SAMPLE_RATE
                || format.getSampleSizeInBits() != 16
                || format.getChannels() != 1
                || format.getFrameSize() != BYTES_PER_SAMPLE
                || format.isBigEndian()) {

            throw new IOException(
                    "Supported audio: WAV PCM signed, "
                            + "44100 Hz, 16-bit, mono, little-endian. "
                            + "Received: " + format
            );
        }
    }

    private void decode(
            byte[] bytes,
            float[] samples,
            int length
    ) {
        for (int i = 0; i < length; i++) {
            int low = bytes[i * 2] & 0xff;
            int high = bytes[i * 2 + 1];

            short value = (short) ((high << 8) | low);

            samples[i] = value / 32768.0f;
        }
    }

    private void encode(
            float[] samples,
            byte[] bytes,
            int length
    ) {
        for (int i = 0; i < length; i++) {
            float clipped = Math.max(
                    -1.0f,
                    Math.min(1.0f, samples[i])
            );

            int value = Math.round(clipped * 32768.0f);
            value = Math.max(-32768, Math.min(32767, value));

            bytes[i * 2] = (byte) (value & 0xff);
            bytes[i * 2 + 1] = (byte) ((value >>> 8) & 0xff);
        }
    }

    private void writeFully(
            SourceDataLine line,
            byte[] bytes,
            int length
    ) throws IOException {

        int offset = 0;

        while (offset < length) {
            int written = line.write(
                    bytes,
                    offset,
                    length - offset
            );

            if (written <= 0) {
                throw new IOException(
                        "Audio output stopped accepting data"
                );
            }

            offset += written;
        }
    }
}