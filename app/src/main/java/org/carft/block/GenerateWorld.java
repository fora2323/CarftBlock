package org.carft.block;

import java.util.Random;
import static org.carft.block.CraftEngine.*;

public class GenerateWorld {
    
    public static void generate(byte[] world, boolean[] exposed, long seed) {
        Random r = new Random(seed);
        double ph1 = r.nextDouble() * 6.28, ph2 = r.nextDouble() * 6.28, ph3 = r.nextDouble() * 6.28;
        
        for (int x = 0; x < WW; x++) {
            for (int z = 0; z < WD; z++) {
                double hg = 14 + 6 * Math.sin(x * 0.1 + ph1) + 4 * Math.cos(z * 0.08 + ph2)
                        + 3 * Math.sin((x + z) * 0.15 + ph3) + 2 * Math.sin(x * 0.3 + z * 0.3);
                int h = Math.max(3, Math.min(WH - 10, (int) hg));
                for (int y = 0; y < h; y++) {
                    int b;
                    if (y < h - 5) b = STONE;
                    else if (y < h - 1) b = (h <= SEA + 1) ? SAND : (h >= 26 ? STONE : DIRT);
                    else b = (h <= SEA + 1) ? SAND : (h >= 26 ? STONE : GRASS);
                    world[(x * WD + z) * WH + y] = (byte) b;
                }
                for (int y = h; y < SEA; y++) {
                    world[(x * WD + z) * WH + y] = (byte) WATER;
                }
            }
        }

        for (int i = 0; i < (WW * WD / 35); i++) {
            int x = 3 + r.nextInt(WW - 6), z = 3 + r.nextInt(WD - 6);
            int y = 0;
            for (int ty = WH - 1; ty >= 0; ty--) {
                int b = world[(x * WD + z) * WH + ty];
                if (b != AIR && b != WATER) {
                    y = ty + 1;
                    break;
                }
            }
            if (y == 0 || world[(x * WD + z) * WH + (y - 1)] != GRASS) continue;

            for (int k = 0; k < 5; k++) {
                world[(x * WD + z) * WH + (y + k)] = (byte) WOOD;
            }
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = 3; dy <= 5; dy++) {
                        if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                        if (dx == 0 && dz == 0 && dy <= 4) continue;
                        int idx = ((x + dx) * WD + (z + dz)) * WH + (y + dy);
                        if (world[idx] == AIR) {
                            world[idx] = (byte) LEAVES;
                        }
                    }
                }
            }
        }

        for (int x = 0; x < WW; x++) {
            for (int y = 0; y < WH; y++) {
                for (int z = 0; z < WD; z++) {
                    int i = (x * WD + z) * WH + y;
                    if (world[i] == AIR) {
                        exposed[i] = false;
                        continue;
                    }
                    boolean exp = false;
                    for (int f = 0; f < 6; f++) {
                        int nx = x + FN[f][0], ny = y + FN[f][1], nz = z + FN[f][2];
                        if (nx < 0 || ny < 0 || nz < 0 || nx >= WW || ny >= WH || nz >= WD) {
                            exp = true;
                            break;
                        }
                        if (!isSolid(world[(nx * WD + nz) * WH + ny])) {
                            exp = true;
                            break;
                        }
                    }
                    exposed[i] = exp;
                }
            }
        }
    }
}