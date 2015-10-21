package com.glview.stackblur;

public interface BlurProcess {
    public byte[] blur(byte[] original, int w, int h, float radius);
    public int[] blur(int[] original, int w, int h, float radius);
}
