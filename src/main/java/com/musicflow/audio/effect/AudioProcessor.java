package com.musicflow.audio.effect;

public interface AudioProcessor {

    void process(float[] samples, int length);
}