package com.musicflow.audio.effect;

public final class EchoDecorator extends AudioEffectDecorator {

    private final float[] delayBuffer;
    private final float strength;
    private int position;

    public EchoDecorator(
            AudioProcessor wrapped,
            int sampleRate,
            int delayMillis,
            float strength
    ) {
        super(wrapped);

        if (sampleRate <= 0) {
            throw new IllegalArgumentException(
                    "Sample rate must be positive"
            );
        }

        if (delayMillis < 1 || delayMillis > 2000) {
            throw new IllegalArgumentException(
                    "Echo delay must be between 1 and 2000 ms"
            );
        }

        if (!Float.isFinite(strength)
                || strength < 0 || strength > 1) {
            throw new IllegalArgumentException(
                    "Echo strength must be between 0 and 1"
            );
        }

        long delaySamples = Math.max(
                1L,
                (long) sampleRate * delayMillis / 1000
        );

        if (delaySamples > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Echo buffer is too large");
        }

        this.delayBuffer = new float[(int) delaySamples];
        this.strength = strength;
    }

    @Override
    protected void applyEffect(float[] samples, int length) {
        for (int i = 0; i < length; i++) {
            float original = samples[i];
            float delayed = delayBuffer[position];

            delayBuffer[position] = original;

            samples[i] = (original + delayed * strength)
                    / (1 + strength);

            position++;

            if (position == delayBuffer.length) {
                position = 0;
            }
        }
    }
}