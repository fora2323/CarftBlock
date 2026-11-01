package org.carft.block;

public class CraftEngine {

    public static final String[] SENS_NAMES = {"Rendah", "Normal", "Tinggi"};
    public static final float[] SENS_VALUES = {0.0025f, 0.004f, 0.0065f};

    public static final int WW = 128, WD = 128, WH = 48, SEA = 10;
    public static final int AIR = 0, GRASS = 1, DIRT = 2, STONE = 3, WOOD = 4, LEAVES = 5,
            SAND = 6, PLANK = 7, COBBLE = 8, WATER = 9;

    public static final int[] COLORS = {0, 0x5DAA3A, 0x8B5A2B, 0x7F7F7F, 0x6B4A2A, 0x2F7D2F,
            0xE0D08A, 0xB8894E, 0x5A5A5A, 0x2E66D9};
    public static final String[] NAMES = {"", "Rumput", "Tanah", "Batu", "Kayu", "Daun",
            "Pasir", "Papan", "Batu bulat", "Air"};
    public static final int[] ALL_BLOCKS = {GRASS, DIRT, STONE, COBBLE, WOOD, PLANK, LEAVES, SAND, WATER};
    public static final int[] DEFAULT_HOTBAR = {GRASS, DIRT, STONE, COBBLE, WOOD, PLANK, LEAVES, SAND, WATER};

    public static final int[][] PAL = {
            {},
            {0x4E9A2E, 0x7DBF4A, 0x3F7F22, 0x9ACB5C, 0xF4E24A, 0xFFFFFF},
            {0x6B4420, 0x9C6B3C, 0x4F3216, 0x7A5030},
            {0x6A6A6A, 0x9A9A9A, 0x4B4B4B, 0xB8B8B8, 0x3A3A3A},
            {0x4A2E17, 0x7A4D26, 0x3B2412},
            {0x1F5A1F, 0x3E8C3E, 0x2A7A2A, 0x0F3D0F, 0x5DB05D},
            {0xF2E1A0, 0xD9C27E, 0xFFF4C4, 0xC9B06A},
            {0xA87A3E, 0xC79A5A, 0x8A5F2C, 0x6E4A22},
            {0x5A5A5A, 0x787878, 0x3E3E3E, 0x8E8E8E, 0x2F2F2F},
            {0x2E66D9, 0x3F7BEA, 0x2456C4, 0x7FB2FF},
    };

    public static final int[] CORNER_ORDER = {0, 1, 3, 2};
    public static final int[][] FN = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    public static final float[] FSHADE = {0.8f, 0.8f, 1.0f, 0.5f, 0.65f, 0.65f};
    public static final int[] BOX_CS = {-1, -1, 1, -1, 1, 1, -1, 1};
    public static final String[] BTN_LABEL = {"TARUH", "HANCUR", "LOMPAT", "MENU"};
    public static final int R_NONE = 0, R_JOY = 1, R_LOOK = 2, R_BTN = 3, R_HOT = 4;
    public static final int WHITE = 0xFFFFFFFF;
    public static final double DAY_SECONDS = 240.0;
    public static final int CLOUD_Y = 38;

    public static final double[][] DIMS = {
            {0.35, 0.27, 0.50, 0.55, 0.22, 0.62, 0.62, 0.22, 0.32},
            {0.40, 0.36, 0.65, 0.75, 0.25, 0.85, 0.90, 0.27, 0.45},
            {0.38, 0.30, 0.55, 0.60, 0.18, 0.70, 0.72, 0.22, 0.36},
    };
    public static final int[] BODY_COL = {0xF2A7B0, 0x7A4B2A, 0xF2F2F2};
    public static final int[] HEAD_COL = {0xF2A7B0, 0x6A3E22, 0x3A3A3A};
    public static final int[] LEG_COL = {0xD98A94, 0x3A2A1A, 0x555555};
    public static final int[] BUTTERFLY_COL = {0xFFD54F, 0xFF7043, 0x64B5F6, 0xF06292, 0xFFFFFF};

    public static boolean isSolid(int b) { return b != AIR && b != WATER; }

    public static float clampF(float v) { return v < -1 ? -1 : (v > 1 ? 1 : v); }
    public static double clampD(double v, double lo, double hi) { return v < lo ? lo : (v > hi ? hi : v); }

    public static int mix(int h) {
        h ^= h >>> 16; h *= 0x7feb352d;
        h ^= h >>> 15; h *= 0x846ca68b;
        h ^= h >>> 16;
        return h;
    }

    public static float rf(int seed) { return (mix(seed) & 0xFFFF) / 65536f; }

    public static int clamp255(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }

    public static int shade(int rgb, float k, int alpha) {
        int r = clamp255((int) (((rgb >> 16) & 255) * k));
        int g = clamp255((int) (((rgb >> 8) & 255) * k));
        int b = clamp255((int) ((rgb & 255) * k));
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lerpColor(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int g = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    public static int blendFog(int blockColor, int skyColor, float fogDepth) {
        fogDepth = Math.max(0f, Math.min(1f, fogDepth));
        int br = (blockColor >> 16) & 255, bg = (blockColor >> 8) & 255, bb = blockColor & 255;
        int sr = (skyColor >> 16) & 255, sg = (skyColor >> 8) & 255, sb = skyColor & 255;

        int r = (int) (br + (sr - br) * fogDepth);
        int g = (int) (bg + (sg - bg) * fogDepth);
        int b = (int) (bb + (sb - bb) * fogDepth);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}