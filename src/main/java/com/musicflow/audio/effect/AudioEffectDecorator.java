package com.musicflow.audio.effect;

import java.util.Objects;

public abstract class AudioEffectDecorator implements AudioProcessor {

    private final AudioProcessor wrapped;

    protected AudioEffectDecorator(AudioProcessor wrapped) {
        this.wrapped = Objects.requireNonNull(
                wrapped,
                "Wrapped processor is required"
        );
    }

    @Override
    public final void process(float[] samples, int length) {
        wrapped.process(samples, length);
        applyEffect(samples, length);
    }

    protected abstract void applyEffect(float[] samples, int length);
}