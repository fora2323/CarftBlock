package org.carft.block;

public class RendererObject {

    public static class Face {
        public float[] xs = new float[4], ys = new float[4];
        public float depth, shade;
        public int color, type, hash, dir;
        public boolean water;
    }

    public static class Animal {
        public int kind;
        public double x, y, z, yaw, speed, walk, flap, tx, ty, tz, baseY;
        public int timer, color;
    }

    public static class Particle {
        public double x, y, z, vx, vy, vz;
        public int life, color;
        public float size;
    }
}