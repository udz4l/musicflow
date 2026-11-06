package com.musicflow.audio.effect;

public final class VolumeDecorator extends AudioEffectDecorator {

    private volatile float gain;

    public VolumeDecorator(AudioProcessor wrapped, float gain) {
        super(wrapped);
        setGain(gain);
    }

    public void setGain(float gain) {
        if (!Float.isFinite(gain) || gain < 0 || gain > 1) {
            throw new IllegalArgumentException(
                    "Volume must be between 0 and 1"
            );
        }

        this.gain = gain;
    }

    @Override
    protected void applyEffect(float[] samples, int length) {
        float currentGain = gain;

        for (int i = 0; i < length; i++) {
            samples[i] *= currentGain;
        }
    }
}