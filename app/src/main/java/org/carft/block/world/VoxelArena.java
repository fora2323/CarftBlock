package org.carft.block.world;

public class VoxelArena {
    public final int width = 64;
    public final int height = 32;
    public final int depth = 64;
    private final byte[] blocks;

    public static final byte AIR = 0;
    public static final byte WALL = 1;
    public static final byte FLOOR = 2;
    public static final byte OBSTACLE = 3;

    public VoxelArena() {
        blocks = new byte[width * height * depth];
        generateArena();
    }

    private void generateArena() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    
                    // Lantai dasar arena
                    if (y == 0) {
                        setBlock(x, y, z, FLOOR);
                    }
                    // Tembok batas keliling arena
                    else if (x == 0 || x == width - 1 || z == 0 || z == depth - 1) {
                        if (y < 8) setBlock(x, y, z, WALL);
                    }
                    // Pilar perlindungan di tengah arena
                    else if (y < 4 && x % 12 == 0 && z % 12 == 0) {
                        setBlock(x, y, z, OBSTACLE);
                        setBlock(x+1, y, z, OBSTACLE);
                        setBlock(x, y, z+1, OBSTACLE);
                        setBlock(x+1, y, z+1, OBSTACLE);
                    }
                }
            }
        }
    }

    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth) return AIR;
        return blocks[(y * width * depth) + (z * width) + x];
    }

    public void setBlock(int x, int y, int z, byte type) {
        if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth) return;
        blocks[(y * width * depth) + (z * width) + x] = type;
    }

    public boolean isSolid(int x, int y, int z) {
        return getBlock(x, y, z) != AIR;
    }
}