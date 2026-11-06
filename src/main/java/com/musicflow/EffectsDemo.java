package com.musicflow;

import com.musicflow.audio.effect.AudioProcessor;
import com.musicflow.audio.effect.DryProcessor;
import com.musicflow.audio.effect.EchoDecorator;
import com.musicflow.audio.effect.VolumeDecorator;

public class EffectsDemo {

    public static void main(String[] args) {
        float[] volumeSamples = {1.0f, -0.8f, 0.4f};

        AudioProcessor volume = new VolumeDecorator(
                new DryProcessor(),
                0.5f
        );

        volume.process(volumeSamples, volumeSamples.length);

        check("Volume changes samples",
                close(volumeSamples[0], 0.5f)
                        && close(volumeSamples[1], -0.4f)
                        && close(volumeSamples[2], 0.2f));

        AudioProcessor echo = new EchoDecorator(
                new DryProcessor(),
                1000,
                2,
                0.5f
        );

        float[] firstBlock = {1.0f, 0.0f};
        float[] secondBlock = {0.0f, 0.0f};

        echo.process(firstBlock, firstBlock.length);
        echo.process(secondBlock, secondBlock.length);

        check("Echo preserves delayed sound between blocks",
                close(firstBlock[0], 2.0f / 3.0f)
                        && close(secondBlock[0], 1.0f / 3.0f));

        AudioProcessor combined = new VolumeDecorator(
                new EchoDecorator(new DryProcessor(), 1000, 2, 0.5f),
                0.5f
        );

        float[] combinedSamples = {1.0f, 0.0f, 0.0f};
        combined.process(combinedSamples, combinedSamples.length);

        check("Decorators can be combined",
                close(combinedSamples[0], 1.0f / 3.0f)
                        && close(combinedSamples[2], 1.0f / 6.0f));

        float[] partialBlock = {0.8f, 0.6f};

        volume.process(partialBlock, 1);

        check("Only valid samples are processed",
                close(partialBlock[0], 0.4f)
                        && close(partialBlock[1], 0.6f));

        boolean rejected = false;

        try {
            new VolumeDecorator(new DryProcessor(), -0.5f);
        } catch (IllegalArgumentException exception) {
            rejected = true;
        }

        check("Invalid volume is rejected", rejected);

        System.out.println("All audio effect checks passed.");
    }

    private static boolean close(float actual, float expected) {
        return Math.abs(actual - expected) < 0.0001f;
    }

    private static void check(String name, boolean passed) {
        if (!passed) {
            throw new IllegalStateException("FAILED: " + name);
        }

        System.out.println("PASS: " + name);
    }
}