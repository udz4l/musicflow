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
import java.util.Arrays;
import java.util.Objects;

public final class PcmAudioPlayer {

    public static final int SAMPLE_RATE = 44100;

    private static final int BLOCK_SAMPLES = 1024;
    private static final int BYTES_PER_SAMPLE = 2;

    public void play(URI audioUri, AudioProcessor processor)
            throws IOException,
            UnsupportedAudioFileException,
            LineUnavailableException {

        play(audioUri, processor, new PlaybackControl());
    }

    public boolean play(
            URI audioUri,
            AudioProcessor processor,
            PlaybackControl control
    ) throws IOException,
            UnsupportedAudioFileException,
            LineUnavailableException {

        Objects.requireNonNull(audioUri, "Audio URI is required");
        Objects.requireNonNull(processor, "Processor is required");
        Objects.requireNonNull(control, "Playback control is required");

        if (!control.awaitReady()) {
            return false;
        }

        try (InputStream input = openInput(audioUri);
             BufferedInputStream buffered = new BufferedInputStream(input);
             AudioInputStream audio =
                     AudioSystem.getAudioInputStream(buffered)) {

            AudioFormat format = audio.getFormat();
            validateFormat(format);
            control.setDurationFrames(audio.getFrameLength());

            if (!control.awaitReady()) {
                return false;
            }

            SourceDataLine line = AudioSystem.getSourceDataLine(format);

            try {
                // Keep the output buffer small for responsive controls.
                line.open(format, BLOCK_SAMPLES * BYTES_PER_SAMPLE * 4);

                if (!control.attach(line)) {
                    return false;
                }

                byte[] bytes = new byte[
                        BLOCK_SAMPLES * BYTES_PER_SAMPLE
                        ];

                float[] samples = new float[BLOCK_SAMPLES];

                while (control.awaitReady()) {
                    int bytesRead = audio.read(bytes, 0, bytes.length);

                    if (bytesRead == -1) {
                        break;
                    }

                    if (bytesRead == 0) {
                        continue;
                    }

                    if (!control.awaitReady()) {
                        return false;
                    }

                    int sampleCount = bytesRead / BYTES_PER_SAMPLE;

                    decode(bytes, samples, sampleCount);
                    processor.process(samples, sampleCount);
                    encode(samples, bytes, sampleCount);

                    if (!writeFully(line, bytes, bytesRead, control)) {
                        return false;
                    }
                }

                if (control.isStopped()) {
                    return false;
                }

                int tailSamples = SAMPLE_RATE * 2;

                while (tailSamples > 0) {
                    if (!control.awaitReady()) {
                        return false;
                    }

                    int count = Math.min(BLOCK_SAMPLES, tailSamples);

                    Arrays.fill(samples, 0, count, 0.0f);
                    processor.process(samples, count);
                    encode(samples, bytes, count);

                    if (!writeFully(
                            line,
                            bytes,
                            count * BYTES_PER_SAMPLE,
                            control
                    )) {
                        return false;
                    }

                    tailSamples -= count;
                }

                if (!control.awaitReady()) {
                    return false;
                }

                line.drain();

                return !control.isStopped();
            } finally {
                control.detach();

                if (line.isOpen()) {
                    line.stop();
                    line.close();
                }
            }
        } catch (IOException
                 | UnsupportedAudioFileException
                 | LineUnavailableException
                 | RuntimeException exception) {

            if (control.isStopped()) {
                return false;
            }

            throw exception;
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

    private void validateFormat(AudioFormat format) throws IOException {
        if (!AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())
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

    private void decode(byte[] bytes, float[] samples, int length) {
        for (int i = 0; i < length; i++) {
            int low = bytes[i * 2] & 0xff;
            int high = bytes[i * 2 + 1];

            short value = (short) ((high << 8) | low);
            samples[i] = value / 32768.0f;
        }
    }

    private void encode(float[] samples, byte[] bytes, int length) {
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

    private boolean writeFully(
            SourceDataLine line,
            byte[] bytes,
            int length,
            PlaybackControl control
    ) throws IOException {

        int offset = 0;

        while (offset < length) {
            if (!control.awaitReady()) {
                return false;
            }

            int written = line.write(bytes, offset, length - offset);

            if (control.isStopped()) {
                return false;
            }

            if (written <= 0) {
                throw new IOException(
                        "Audio output stopped accepting data"
                );
            }

            offset += written;
        }

        return true;
    }
}