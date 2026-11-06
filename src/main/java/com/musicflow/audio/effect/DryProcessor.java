package com.musicflow.audio.effect;

import java.util.Objects;

public final class DryProcessor implements AudioProcessor {

    @Override
    public void process(float[] samples, int length) {
        Objects.requireNonNull(samples, "Samples are required");

        if (length < 0 || length > samples.length) {
            throw new IllegalArgumentException("Invalid sample count");
        }
    }
}