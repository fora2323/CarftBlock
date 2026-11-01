package org.carft.block;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.EditText;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import static org.carft.block.CraftEngine.*;
import org.carft.block.RendererObject.*;

public class Game extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public static int sensIndex = 1;
    public static float lookSens = 0.004f;
    public static int renderDist = 20;

    final Activity owner;
    final Object lock = new Object();
    final Random rnd = new Random();
    byte[] world = new byte[WW * WD * WH];
    boolean[] exposed = new boolean[WW * WD * WH];

    volatile double dayT = 0.15;
    volatile boolean raining = false;
    volatile float rainLevel = 0f;
    double cloudOff = 0, rainT = 0;
    volatile String notice = "";
    volatile long noticeUntil = 0;

    double px, py, pz, vy;
    boolean onGround;
    double yaw = 0, pitch = 0;
    double bobPhase = 0;
    float walkAmt = 0;
    double camY = 0, camVel = 0, shakeAmp = 0, shakeT = 0;

    int[] hotbar = DEFAULT_HOTBAR.clone();
    int sel = 0;
    boolean invOpen = false;

    final ArrayList<Animal> animals = new ArrayList<Animal>();
    final ArrayList<Particle> parts = new ArrayList<Particle>();

    int viewW, viewH;
    float dpadX, dpadY, dpadR;
    float btnR, hotSlot, hotGap, hotX0, hotY0;
    float joyX, joyY;
    boolean joyActive;
    float[] btnX = new float[4], btnY = new float[4];
    int[] role = new int[16];
    float[] tx = new float[16], ty = new float[16];

    final RectF chatBtn = new RectF(), exitBtn = new RectF(), invClose = new RectF();
    final RectF[] invTiles = {new RectF(), new RectF(), new RectF(), new RectF(), new RectF(),
            new RectF(), new RectF(), new RectF(), new RectF()};

    final Paint paint = new Paint();
    final Path path = new Path();
    final ArrayList<Face> faces = new ArrayList<Face>();
    int faceCount = 0;
    final int[] hitBuf = new int[6];
    final double[] q = new double[12];
    final double[] pt = new double[3];
    final double[] cc = new double[3];
    final float[] sxs = new float[4], sys = new float[4];
    double sy, cy, sp, cp, focal, hw, hh, eX, eY, eZ, depthAcc;
    float light;

    Thread thread;
    volatile boolean running;

    Bitmap guiBmp;
    Bitmap[] texBlock = new Bitmap[10];
    Bitmap texGrassSide;
    Bitmap texWoodTop;

    final Matrix texMatrix = new Matrix();
    final float[] srcPoly = new float[8];
    final float[] dstPoly = new float[8];

    final Rect[] SPRITES = {
            new Rect(104, 105, 130, 131),
            new Rect(52, 105, 78, 131),
            new Rect(0, 105, 26, 131),
            new Rect(200, 64, 226, 90)
    };

    final Comparator<Face> depthCmp = new Comparator<Face>() {
        public int compare(Face a, Face b) {
            return Float.compare(b.depth, a.depth);
        }
    };

    public Game(Activity owner) {
        super(owner);
        this.owner = owner;
        getHolder().addCallback(this);
        setFocusable(true);

        paint.setFilterBitmap(false);
        paint.setAntiAlias(false);
        paint.setDither(false);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        opts.inDither = false;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

        try {
            InputStream is = owner.getAssets().open("gui/gui.png");
            guiBmp = BitmapFactory.decodeStream(is, null, opts);
            is.close();
        } catch (Exception e) {}

        try {
            texBlock[GRASS] = BitmapFactory.decodeResource(getResources(), R.drawable.grass_carried, opts);
            texGrassSide = BitmapFactory.decodeResource(getResources(), R.drawable.grass_side_carried, opts);
            texBlock[DIRT] = BitmapFactory.decodeResource(getResources(), R.drawable.dirt, opts);
            texBlock[STONE] = BitmapFactory.decodeResource(getResources(), R.drawable.stone, opts);
            texBlock[SAND] = BitmapFactory.decodeResource(getResources(), R.drawable.sand, opts);
            texBlock[WATER] = BitmapFactory.decodeResource(getResources(), R.drawable.water_still, opts);
            texBlock[WOOD] = BitmapFactory.decodeResource(getResources(), R.drawable.log_oak, opts);
            texWoodTop = BitmapFactory.decodeResource(getResources(), R.drawable.log_oak_top, opts);
            texBlock[LEAVES] = BitmapFactory.decodeResource(getResources(), R.drawable.leaves_oak_opaque, opts);

            texBlock[PLANK] = texBlock[DIRT];
            texBlock[COBBLE] = texBlock[STONE];
        } catch (Exception e) {}

        GenerateWorld.generate(world, exposed, System.currentTimeMillis());
        px = WW / 2 + 0.5;
        pz = WD / 2 + 0.5;
        py = surfaceY((int) px, (int) pz);
        spawnAnimals();
    }

    int idx(int x, int y, int z) { return (x * WD + z) * WH + y; }

    void text(Canvas c, String s, float x, float y, float size, int color, Paint.Align al, boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(size);
        paint.setTextAlign(al);
        paint.setFakeBoldText(bold);
        paint.setColor(0x99000000);
        c.drawText(s, x + size * 0.06f, y + size * 0.06f, paint);
        paint.setColor(color);
        c.drawText(s, x, y, paint);
        paint.setFakeBoldText(false);
    }

    float daylight() {
        double s = Math.sin(2 * Math.PI * dayT);
        return (float) Math.max(0, Math.min(1, s * 2.5 + 0.02));
    }

    String timeName() {
        double t = dayT;
        if (t < 0.05 || t >= 0.95) return "Fajar";
        if (t < 0.45) return "Siang";
        if (t < 0.55) return "Senja";
        return "Malam";
    }

    int getBlock(int x, int y, int z) {
        if (y < 0) return STONE;
        if (y >= WH) return AIR;
        if (x < 0 || z < 0 || x >= WW || z >= WD) return STONE;
        return world[idx(x, y, z)];
    }

    void setBlock(int x, int y, int z, int b) {
        if (x < 0 || y < 0 || z < 0 || x >= WW || y >= WH || z >= WD) return;
        world[idx(x, y, z)] = (byte) b;
        updateExposed(x, y, z);
        for(int f=0; f<6; f++) updateExposed(x+FN[f][0], y+FN[f][1], z+FN[f][2]);
    }

    void updateExposed(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= WW || y >= WH || z >= WD) return;
        int i = idx(x, y, z);
        int b = world[i];
        if (b == AIR) { exposed[i] = false; return; }
        boolean exp = false;
        for(int f=0; f<6; f++){
            int nx = x+FN[f][0], ny = y+FN[f][1], nz = z+FN[f][2];
            if (nx<0||ny<0||nz<0||nx>=WW||ny>=WH||nz>=WD) { exp = true; break; }
            if (!isSolid(world[idx(nx,ny,nz)])) { exp = true; break; }
        }
        exposed[i] = exp;
    }

    double surfaceY(int x, int z) {
        for (int y = WH - 1; y >= 0; y--)
            if (isSolid(getBlock(x, y, z))) return y + 1;
        return 1;
    }

    void spawnAnimals() {
        animals.clear();
        int tries = 0;
        while (animals.size() < 12 && tries < 1000) {
            tries++;
            int x = 4 + rnd.nextInt(WW - 8), z = 4 + rnd.nextInt(WD - 8);
            int y = (int) surfaceY(x, z);
            if (getBlock(x, y - 1, z) != GRASS) continue;
            Animal a = new Animal();
            a.kind = rnd.nextInt(3);
            a.x = x + 0.5; a.y = y; a.z = z + 0.5;
            a.yaw = rnd.nextDouble() * Math.PI * 2;
            a.timer = rnd.nextInt(80);
            animals.add(a);
        }
        int made = 0; tries = 0;
        while (made < 15 && tries < 1000) {
            tries++;
            int x = 4 + rnd.nextInt(WW - 8), z = 4 + rnd.nextInt(WD - 8);
            int y = (int) surfaceY(x, z);
            if (getBlock(x, y - 1, z) != GRASS) continue;
            Animal a = new Animal();
            a.kind = 3;
            a.x = x + 0.5; a.y = y + 2; a.z = z + 0.5;
            a.baseY = a.y;
            a.tx = a.x; a.ty = a.y; a.tz = a.z;
            a.color = BUTTERFLY_COL[rnd.nextInt(BUTTERFLY_COL.length)];
            animals.add(a);
            made++;
        }
    }

    boolean solidAt(double x, double y, double z) {
        return isSolid(getBlock((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)));
    }

    boolean collides(double x, double y, double z) {
        double r = 0.45;
        for (int i = 0; i < 4; i++) {
            double cx = x + ((i & 1) == 0 ? -r : r);
            double cz = z + ((i & 2) == 0 ? -r : r);
            if (solidAt(cx, y + 0.01, cz) || solidAt(cx, y + 1.7, cz)) return true;
        }
        return false;
    }

    void updatePlayer() {
        double spd = 0.095;
        double fwd = -joyY, str = joyX;
        double mx = (Math.sin(yaw) * fwd + Math.cos(yaw) * str) * spd;
        double mz = (Math.cos(yaw) * fwd - Math.sin(yaw) * str) * spd;
        if (!collides(px + mx, py, pz)) px += mx;
        if (!collides(px, py, pz + mz)) pz += mz;

        vy -= 0.025;
        if (vy < -0.7) vy = -0.7;
        double ny = py + vy;
        if (collides(px, ny, pz)) {
            if (vy < 0 && !onGround && vy < -0.12) {
                camVel -= -vy * 0.6;
                shakeAmp = Math.min(0.1, -vy * 0.4);
            }
            if (vy < 0) onGround = true;
            vy = 0;
        } else {
            py = ny;
            onGround = false;
        }

        boolean moving = onGround && (Math.abs(joyX) + Math.abs(joyY)) > 0.15f;
        walkAmt += ((moving ? 1f : 0f) - walkAmt) * 0.12f;
        if (moving) bobPhase += 0.22;
    }

    void updateCamera() {
        camVel += -camY * 0.12;
        camVel *= 0.8;
        camY += camVel;
        shakeAmp *= 0.92;
        shakeT += 0.5;
    }

    void updateBlocks() {
        for (int i = 0; i < 40; i++) {
            int x = rnd.nextInt(WW);
            int y = rnd.nextInt(WH);
            int z = rnd.nextInt(WD);
            if (getBlock(x, y, z) == WATER) {
                if (y > 0 && getBlock(x, y - 1, z) == AIR) {
                    setBlock(x, y - 1, z, WATER);
                } else if (y > 0 && isSolid(getBlock(x, y - 1, z))) {
                    int dir = rnd.nextInt(4);
                    int nx = x + (dir == 0 ? 1 : (dir == 1 ? -1 : 0));
                    int nz = z + (dir == 2 ? 1 : (dir == 3 ? -1 : 0));
                    if (getBlock(nx, y, nz) == AIR) {
                        setBlock(nx, y, nz, WATER);
                    }
                }
            }
        }
    }

    boolean raycast(int[] out) {
        double ex = px, ey = py + 1.6, ez = pz;
        double dx = Math.sin(yaw) * Math.cos(pitch);
        double dy = Math.sin(pitch);
        double dz = Math.cos(yaw) * Math.cos(pitch);
        int lx = Integer.MIN_VALUE, ly = 0, lz = 0;
        for (double t = 0; t < 6; t += 0.05) {
            int x = (int) Math.floor(ex + dx * t);
            int y = (int) Math.floor(ey + dy * t);
            int z = (int) Math.floor(ez + dz * t);
            if (x == lx && y == ly && z == lz) continue;
            if (isSolid(getBlock(x, y, z))) {
                out[0] = x; out[1] = y; out[2] = z;
                out[3] = lx; out[4] = ly; out[5] = lz;
                return true;
            }
            lx = x; ly = y; lz = z;
        }
        return false;
    }

    void breakBlock() {
        if (!raycast(hitBuf)) return;
        int x = hitBuf[0], y = hitBuf[1], z = hitBuf[2];
        int b = getBlock(x, y, z);
        setBlock(x, y, z, AIR);
        spawnParticles(x, y, z, b);
    }

    void placeBlock() {
        if (!raycast(hitBuf)) return;
        int x = hitBuf[3], y = hitBuf[4], z = hitBuf[5];
        if (x == Integer.MIN_VALUE) return;
        int cur = getBlock(x, y, z);
        if (cur != AIR && cur != WATER) return;
        int fx = (int) Math.floor(px), fz = (int) Math.floor(pz);
        if (x == fx && z == fz && (y == (int) Math.floor(py) || y == (int) Math.floor(py + 1.7))) return;
        setBlock(x, y, z, hotbar[sel]);
    }

    void spawnParticles(int x, int y, int z, int b) {
        if (b <= 0 || b >= PAL.length) return;
        if (parts.size() > 300) return;
        for (int i = 0; i < 16; i++) {
            Particle p = new Particle();
            p.x = x + 0.2 + rnd.nextDouble() * 0.6;
            p.y = y + 0.2 + rnd.nextDouble() * 0.6;
            p.z = z + 0.2 + rnd.nextDouble() * 0.6;
            p.vx = (rnd.nextDouble() - 0.5) * 0.12;
            p.vy = rnd.nextDouble() * 0.1;
            p.vz = (rnd.nextDouble() - 0.5) * 0.12;
            int[] pal = PAL[b];
            p.color = pal[rnd.nextInt(pal.length)];
            p.life = 25 + rnd.nextInt(15);
            p.size = 0.6f + rnd.nextFloat() * 0.6f;
            parts.add(p);
        }
    }

    void updateParticles() {
        for (int i = parts.size() - 1; i >= 0; i--) {
            Particle p = parts.get(i);
            p.x += p.vx; p.y += p.vy; p.z += p.vz;
            p.vy -= 0.006;
            p.life--;
            if (p.life <= 0) parts.remove(i);
        }
    }

    void press(int b) {
        if (b == 0) placeBlock();
        else if (b == 1) breakBlock();
        else if (b == 2) {
            if (onGround) {
                vy = 0.25;
                onGround = false;
                camVel -= 0.06;
                shakeAmp = Math.max(shakeAmp, 0.03);
            }
        } else if (b == 3) openInventory();
    }

    void openInventory() {
        invOpen = true;
        joyActive = false;
        joyX = 0; joyY = 0;
        for (int i = 0; i < 16; i++) role[i] = R_NONE;
    }

    void setupCamera() {
        double bobY = Math.sin(bobPhase * 2) * 0.04 * walkAmt;
        eX = px;
        eY = py + 1.6 + camY + bobY;
        eZ = pz;
        double yy = yaw + Math.sin(shakeT) * shakeAmp * 0.6;
        double pp = pitch + Math.sin(shakeT * 1.3) * shakeAmp * 0.8 + Math.sin(bobPhase) * 0.01 * walkAmt;
        sy = Math.sin(yy); cy = Math.cos(yy);
        sp = Math.sin(pp); cp = Math.cos(pp);
        focal = viewH * 1.1;
        hw = viewW / 2.0;
        hh = viewH / 2.0;
        light = (0.3f + 0.7f * daylight()) * (raining ? 0.8f : 1f) * (1f - 0.2f * rainLevel);
    }

    boolean projectPoint(int j, double x, double y, double z) {
        double dx = x - eX, dy = y - eY, dz = z - eZ;
        double zz = dx * sy + dz * cy;
        double xx = dx * cy - dz * sy;
        double zc = zz * cp + dy * sp;
        double vc = -zz * sp + dy * cp;
        if (zc < 0.15) return false;
        sxs[j] = (float) (hw + focal * xx / zc);
        sys[j] = (float) (hh - focal * vc / zc);
        depthAcc += zc;
        return true;
    }

    void addQuad(double nx, double ny, double nz, int col, int type, int hash, int dir, float fs, double maxD) {
        double mx = (q[0] + q[3] + q[6] + q[9]) * 0.25 - eX;
        double my = (q[1] + q[4] + q[7] + q[10]) * 0.25 - eY;
        double mz = (q[2] + q[5] + q[8] + q[11]) * 0.25 - eZ;
        if (mx * nx + my * ny + mz * nz >= 0) return;
        depthAcc = 0;
        for (int j = 0; j < 4; j++)
            if (!projectPoint(j, q[j * 3], q[j * 3 + 1], q[j * 3 + 2])) return;
        float depth = (float) (depthAcc / 4);
        if (depth > maxD) return;

        if (faceCount == faces.size()) faces.add(new Face());
        Face f = faces.get(faceCount++);
        for (int j = 0; j < 4; j++) { f.xs[j] = sxs[j]; f.ys[j] = sys[j]; }
        f.depth = depth;
        f.color = col;
        f.type = type;
        f.hash = hash;
        f.dir = dir;
        f.water = type == WATER;
        f.shade = fs * light;
    }

    void addBlock(int x, int y, int z, int b) {
        int hash = mix(x * 73856093 ^ y * 19349663 ^ z * 83492791);
        for (int f = 0; f < 6; f++) {
            int nx = FN[f][0], ny = FN[f][1], nz = FN[f][2];
            int nb = getBlock(x + nx, y + ny, z + nz);
            if (isSolid(nb)) continue;
            if (b == WATER && nb == WATER) continue;
            int a = f / 2, u = (a + 1) % 3, v = (a + 2) % 3;
            cc[0] = x + 0.5; cc[1] = y + 0.5; cc[2] = z + 0.5;
            cc[a] += FN[f][a] * 0.5;
            for (int j = 0; j < 4; j++) {
                int k = CORNER_ORDER[j];
                pt[0] = cc[0]; pt[1] = cc[1]; pt[2] = cc[2];
                pt[u] += ((k & 1) == 0) ? -0.5 : 0.5;
                pt[v] += ((k & 2) == 0) ? -0.5 : 0.5;
                if (b == WATER && f == 2) pt[1] -= 0.12;
                q[j * 3] = pt[0]; q[j * 3 + 1] = pt[1]; q[j * 3 + 2] = pt[2];
            }
            addQuad(nx, ny, nz, COLORS[b], b, hash, f, FSHADE[f], renderDist + 1);
        }
    }

    void addClouds() {
        int i0 = (int) Math.floor((px - 50 - cloudOff) / 4), i1 = (int) Math.floor((px + 50 - cloudOff) / 4);
        int k0 = (int) Math.floor((pz - 50) / 4), k1 = (int) Math.floor((pz + 50) / 4);
        for (int i = i0; i <= i1; i++)
            for (int k = k0; k <= k1; k++) {
                int h = mix(i * 73856093 ^ k * 19349663 ^ 0x5A5A);
                if ((h & 0xFF) > 90) continue;
                double x0 = i * 4 + cloudOff, z0 = k * 4, x1 = x0 + 4, z1 = z0 + 4;
                double yb = CLOUD_Y, yt = CLOUD_Y + 1;
                setQ(x0, yb, z0, x1, yb, z0, x1, yb, z1, x0, yb, z1);
                addQuad(0, -1, 0, 0xFFFFFF, -2, h, 0, 1f, 100);
                setQ(x0, yt, z0, x1, yt, z0, x1, yt, z1, x0, yt, z1);
                addQuad(0, 1, 0, 0xFFFFFF, -2, h, 1, 1f, 100);
            }
    }

    void setQ(double ax, double ay, double az, double bx, double by, double bz,
              double cx, double cy2, double cz, double dx, double dy, double dz) {
        q[0] = ax; q[1] = ay; q[2] = az;
        q[3] = bx; q[4] = by; q[5] = bz;
        q[6] = cx; q[7] = cy2; q[8] = cz;
        q[9] = dx; q[10] = dy; q[11] = dz;
    }

    void collectFaces() {
        faceCount = 0;
        int R = renderDist;
        int fx = (int) Math.floor(px), fy = (int) Math.floor(py), fz = (int) Math.floor(pz);
        int x0 = Math.max(0, fx - R), x1 = Math.min(WW - 1, fx + R);
        int y0 = Math.max(0, fy - R), y1 = Math.min(WH - 1, fy + R);
        int z0 = Math.max(0, fz - R), z1 = Math.min(WD - 1, fz + R);
        double ex = px, ey = py + 1.6, ez = pz;
        
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    int id = idx(x, y, z);
                    if (!exposed[id]) continue;
                    
                    int b = world[id];
                    if (b == AIR) continue;
                    
                    double bx = x + 0.5 - ex, by = y + 0.5 - ey, bz = z + 0.5 - ez;
                    if (bx * bx + by * by + bz * bz > R * R) continue;
                    
                    addBlock(x, y, z, b);
                }
            }
        }
        addClouds();
        addEntities();
    }

    void addBox(double cx, double cy, double cz, double hx, double hy, double hz, double yaw, int col) {
        double C = Math.cos(yaw), S = Math.sin(yaw);
        double[] h = {hx, hy, hz};
        for (int a = 0; a < 3; a++)
            for (int s = -1; s <= 1; s += 2) {
                int u = (a + 1) % 3, v = (a + 2) % 3;
                for (int j = 0; j < 4; j++) {
                    pt[0] = 0; pt[1] = 0; pt[2] = 0;
                    pt[a] = s * h[a];
                    pt[u] = BOX_CS[2 * j] * h[u];
                    pt[v] = BOX_CS[2 * j + 1] * h[v];
                    double wx = pt[0] * C + pt[2] * S;
                    double wz = -pt[0] * S + pt[2] * C;
                    q[j * 3] = cx + wx;
                    q[j * 3 + 1] = cy + pt[1];
                    q[j * 3 + 2] = cz + wz;
                }
                double nl0 = a == 0 ? s : 0, nl1 = a == 1 ? s : 0, nl2 = a == 2 ? s : 0;
                double wnx = nl0 * C + nl2 * S;
                double wnz = -nl0 * S + nl2 * C;
                addQuad(wnx, nl1, wnz, col, -1, 0, a * 2 + (s > 0 ? 1 : 0), a == 1 ? 1f : 0.75f, 25);
            }
    }

    void partAt(double bx, double by, double bz, double yaw,
                double lx, double ly, double lz, double hx, double hy, double hz, int col) {
        double C = Math.cos(yaw), S = Math.sin(yaw);
        double cx = bx + lx * C + lz * S;
        double cz = bz - lx * S + lz * C;
        addBox(cx, by + ly, cz, hx, hy, hz, yaw, col);
    }

    void addEntities() {
        for (Animal a : animals) {
            double dx = a.x - px, dz = a.z - pz;
            if (dx * dx + dz * dz > 400) continue;
            if (a.kind == 3) drawButterfly(a);
            else drawAnimal(a);
        }
    }

    void drawAnimal(Animal a) {
        double[] d = DIMS[a.kind];
        double legH = d[3] - d[1];
        double swing = Math.sin(a.walk) * 0.06 * (a.speed > 0 ? 1 : 0);
        partAt(a.x, a.y, a.z, a.yaw, 0, d[3], 0, d[0], d[1], d[2], BODY_COL[a.kind]);
        partAt(a.x, a.y, a.z, a.yaw, 0, d[6], d[5], d[4], d[4], d[4], HEAD_COL[a.kind]);
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2)
                partAt(a.x, a.y, a.z, a.yaw, sx * d[7], legH / 2, sz * d[8] + swing * sz,
                        0.08, legH / 2, 0.08, LEG_COL[a.kind]);
        if (a.kind == 1) {
            partAt(a.x, a.y, a.z, a.yaw, d[0] + 0.01, d[3] + 0.05, -0.1, 0.01, 0.15, 0.2, 0xF4F4F4);
        }
    }

    void drawButterfly(Animal a) {
        double w = 0.03 + 0.1 * Math.abs(Math.cos(a.flap));
        partAt(a.x, a.y, a.z, a.yaw, 0, 0, 0, 0.02, 0.02, 0.12, 0x222222);
        partAt(a.x, a.y, a.z, a.yaw, w, 0, 0, w, 0.008, 0.1, a.color);
        partAt(a.x, a.y, a.z, a.yaw, -w, 0, 0, w, 0.008, 0.1, a.color);
    }

    void updateAnimals() {
        for (Animal a : animals) {
            if (a.kind == 3) {
                a.timer--;
                if (a.timer <= 0) {
                    a.tx = clampD(a.x + (rnd.nextDouble() * 2 - 1) * 4, 2, WW - 2);
                    a.tz = clampD(a.z + (rnd.nextDouble() * 2 - 1) * 4, 2, WD - 2);
                    a.ty = a.baseY + 1 + rnd.nextDouble() * 3;
                    a.timer = 60 + rnd.nextInt(120);
                }
                double dx = a.tx - a.x, dz = a.tz - a.z;
                a.x += dx * 0.02;
                a.z += dz * 0.02;
                a.y += (a.ty - a.y) * 0.03;
                a.yaw = Math.atan2(dx, dz);
                a.flap += 0.45;
                continue;
            }
            if (--a.timer <= 0) {
                if (rnd.nextInt(3) == 0) {
                    a.speed = 0;
                    a.timer = 40 + rnd.nextInt(80);
                } else {
                    a.speed = 0.035;
                    a.yaw = rnd.nextDouble() * Math.PI * 2;
                    a.timer = 60 + rnd.nextInt(120);
                }
            }
            if (a.speed > 0) {
                double nx = a.x + Math.sin(a.yaw) * a.speed;
                double nz = a.z + Math.cos(a.yaw) * a.speed;
                int fx = (int) Math.floor(nx), fz = (int) Math.floor(nz);
                if (fx < 1 || fz < 1 || fx >= WW - 1 || fz >= WD - 1) {
                    a.yaw += Math.PI;
                    a.timer = 20;
                } else {
                    int top = (int) surfaceY(fx, fz);
                    if (getBlock(fx, top, fz) == WATER || top - a.y > 1.1 || a.y - top > 2.5) {
                        a.yaw += 1.2 + rnd.nextDouble() * 2;
                        a.timer = 25;
                    } else {
                        a.x = nx; a.z = nz; a.y = top;
                    }
                }
                a.walk += 0.3;
            }
        }
    }

    void drawSky(Canvas c, int currentSkyColor) {
        float day = daylight();
        c.drawColor(currentSkyColor);

        int w = viewW, h = viewH;
        float horizon = h * 0.72f;
        paint.setStyle(Paint.Style.FILL);

        if (day < 0.5f) {
            int al = (int) ((0.5f - day) * 2 * 200);
            for (int i = 0; i < 80; i++) {
                float sx = rf(i * 3 + 101) * w, sy2 = rf(i * 3 + 202) * horizon;
                paint.setColor((al << 24) | 0xFFFFFF);
                c.drawRect(sx, sy2, sx + 2, sy2 + 2, paint);
            }
        }

        if (dayT < 0.5) {
            double t = dayT * 2;
            float sx = (float) (w * (0.9 - 0.8 * t));
            float sy2 = (float) (horizon - Math.sin(Math.PI * t) * horizon * 0.8);
            paint.setColor(0x44FFE08A);
            c.drawCircle(sx, sy2, h * 0.11f, paint);
            paint.setColor(0xFFFFE08A);
            c.drawCircle(sx, sy2, h * 0.07f, paint);
        } else {
            double t = (dayT - 0.5) * 2;
            float sx = (float) (w * (0.9 - 0.8 * t));
            float sy2 = (float) (horizon - Math.sin(Math.PI * t) * horizon * 0.8);
            paint.setColor(0x33E8EEFF);
            c.drawCircle(sx, sy2, h * 0.09f, paint);
            paint.setColor(0xFFE8EEFF);
            c.drawCircle(sx, sy2, h * 0.05f, paint);
        }
    }

    void drawTexturedQuad(Canvas c, Face f, Bitmap texture, float fogIntensity, int currentSkyColor) {
        if (texture == null) return;
        
        float w = texture.getWidth();
        float h = texture.getHeight();
        
        srcPoly[0] = 0; srcPoly[1] = h;
        srcPoly[2] = 0; srcPoly[3] = 0;
        srcPoly[4] = w; srcPoly[5] = 0;
        srcPoly[6] = w; srcPoly[7] = h;
        
        dstPoly[0] = f.xs[0]; dstPoly[1] = f.ys[0];
        dstPoly[2] = f.xs[1]; dstPoly[3] = f.ys[1];
        dstPoly[4] = f.xs[2]; dstPoly[5] = f.ys[2];
        dstPoly[6] = f.xs[3]; dstPoly[7] = f.ys[3];
        
        texMatrix.setPolyToPoly(srcPoly, 0, dstPoly, 0, 4);
        
        if (f.water) {
            paint.setAlpha(170);
        }
        
        c.drawBitmap(texture, texMatrix, paint);
        
        if (f.water) {
            paint.setAlpha(255);
        }
        
        int alphaShade = (int)((1f - f.shade) * 255);
        if (alphaShade > 0 || fogIntensity > 0) {
            path.reset();
            path.moveTo(f.xs[0], f.ys[0]);
            path.lineTo(f.xs[1], f.ys[1]);
            path.lineTo(f.xs[2], f.ys[2]);
            path.lineTo(f.xs[3], f.ys[3]);
            path.close();
            
            paint.setStyle(Paint.Style.FILL);
            if (alphaShade > 0) {
                paint.setColor((alphaShade << 24));
                c.drawPath(path, paint);
            }
            if (fogIntensity > 0) {
                paint.setColor((int)(fogIntensity * 255) << 24 | (currentSkyColor & 0xFFFFFF));
                c.drawPath(path, paint);
            }
        }
    }

    void drawFrame(Canvas c) {
        setupCamera();

        float day = daylight();
        int currentSkyColor = lerpColor(0xFF0B1030, 0xFF8ECFFF, day);
        if (rainLevel > 0.01f) currentSkyColor = lerpColor(currentSkyColor, 0xFF5E6B7A, rainLevel * 0.6f);

        drawSky(c, currentSkyColor);
        collectFaces();
        Collections.sort(faces.subList(0, faceCount), depthCmp);

        for (int i = 0; i < faceCount; i++) {
            Face f = faces.get(i);
            float fogIntensity = Math.max(0f, Math.min(1f, (f.depth - (renderDist * 0.45f)) / (renderDist * 0.55f)));

            if (f.type > 0 && texBlock[f.type] != null) {
                Bitmap tex = texBlock[f.type];
                
                if (f.type == GRASS) {
                    if (f.dir == 2) tex = texBlock[GRASS];
                    else if (f.dir == 3) tex = texBlock[DIRT];
                    else tex = texGrassSide;
                } else if (f.type == WOOD) {
                    if (f.dir == 2 || f.dir == 3) tex = texWoodTop;
                    else tex = texBlock[WOOD];
                }
                
                drawTexturedQuad(c, f, tex, fogIntensity, currentSkyColor);
            } else {
                path.reset();
                path.moveTo(f.xs[0], f.ys[0]);
                path.lineTo(f.xs[1], f.ys[1]);
                path.lineTo(f.xs[2], f.ys[2]);
                path.lineTo(f.xs[3], f.ys[3]);
                path.close();

                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                int baseShaded = shade(f.color, f.shade, 0xFF);
                if (f.type == -2) paint.setColor(baseShaded);
                else paint.setColor(blendFog(baseShaded, currentSkyColor, fogIntensity));
                c.drawPath(path, paint);
            }
        }

        drawParticles(c);
        drawRain(c);
        drawHud(c);
        if (invOpen) drawInventory(c);
    }

    void drawParticles(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        for (Particle p : parts) {
            depthAcc = 0;
            if (!projectPoint(0, p.x, p.y, p.z)) continue;
            float s = (float) Math.max(2, viewH * 0.03 / depthAcc) * p.size;
            int alpha = (int) (255 * Math.min(1f, p.life / 10f));
            paint.setColor(shade(p.color, 1f, alpha));
            c.drawRect(sxs[0] - s, sys[0] - s, sxs[0] + s, sys[0] + s, paint);
        }
    }

    void drawRain(Canvas c) {
        if (rainLevel < 0.05f) return;
        int n = (int) (160 * rainLevel);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(0x99C8E6FF);
        for (int i = 0; i < n; i++) {
            double rx = px + (rf(i * 3 + 11) * 2 - 1) * 12;
            double rz = pz + (rf(i * 5 + 7) * 2 - 1) * 12;
            double fall = (rainT * 0.25 + rf(i * 7 + 3) * 14) % 14;
            double ry = py + 1.6 + 10 - fall;
            depthAcc = 0;
            if (!projectPoint(0, rx, ry, rz)) continue;
            if (!projectPoint(1, rx, ry - 0.5, rz)) continue;
            c.drawLine(sxs[0], sys[0], sxs[1], sys[1], paint);
        }
    }

    void drawHud(Canvas c) {
        float cx = viewW / 2f, cy = viewH / 2f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(WHITE);
        c.drawLine(cx - 15, cy, cx + 15, cy, paint);
        c.drawLine(cx, cy - 15, cx, cy + 15, paint);

        text(c, "Waktu: " + timeName() + "   Cuaca: " + (raining ? "Hujan" : "Terang"),
                20, viewH * 0.08f, viewH * 0.045f, WHITE, Paint.Align.LEFT, true);
        text(c, "Kiri: jalan   Kanan geser: lihat", 20, viewH - 20, viewH * 0.035f,
                WHITE, Paint.Align.LEFT, false);

        drawBox(c, chatBtn, "CHAT", 0xAA3A3F45);
        drawBox(c, exitBtn, "KELUAR", 0xAA8B2A2A);

        GUI.drawDPad(c, paint, dpadX, dpadY, dpadR, joyX, joyY, joyActive);

        for (int i = 0; i < 4; i++) {
            boolean isPressed = false;
            for (int p = 0; p < 16; p++) {
                if (role[p] == R_BTN && buttonAt(tx[p], ty[p]) == i) {
                    isPressed = true; break;
                }
            }
            GUI.drawActionButton(c, paint, btnX[i], btnY[i], btnR, BTN_LABEL[i], isPressed);
        }

        drawHotbar(c);

        if (System.currentTimeMillis() < noticeUntil) {
            text(c, notice, viewW / 2f, hotY0 - viewH * 0.03f, viewH * 0.045f,
                    0xFFFFE066, Paint.Align.CENTER, true);
        }
    }

    void drawHotbar(Canvas c) {
        for (int i = 0; i < 9; i++) {
            float x = hotX0 + i * (hotSlot + hotGap);
            int blk = hotbar[i];
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x88000000);
            c.drawRoundRect(x, hotY0, x + hotSlot, hotY0 + hotSlot, 10, 10, paint);

            if (texBlock[blk] != null) {
                Bitmap tex = texBlock[blk];
                if (blk == GRASS) tex = texGrassSide;
                c.drawBitmap(tex, null, new RectF(x + hotSlot * 0.15f, hotY0 + hotSlot * 0.15f, x + hotSlot * 0.85f, hotY0 + hotSlot * 0.85f), paint);
            } else {
                paint.setColor(COLORS[blk]);
                c.drawRect(x + hotSlot * 0.18f, hotY0 + hotSlot * 0.18f,
                        x + hotSlot * 0.82f, hotY0 + hotSlot * 0.82f, paint);
            }
            
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(i == sel ? 4f : 2f);
            paint.setColor(i == sel ? WHITE : 0x66FFFFFF);
            c.drawRoundRect(x, hotY0, x + hotSlot, hotY0 + hotSlot, 10, 10, paint);
            text(c, String.valueOf(i + 1), x + 6, hotY0 + hotSlot * 0.3f, hotSlot * 0.22f,
                    WHITE, Paint.Align.LEFT, false);
        }
    }

    void drawInventory(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xCC101418);
        c.drawRect(0, 0, viewW, viewH, paint);
        text(c, "Pilih blok untuk slot " + (sel + 1), viewW / 2f, viewH * 0.14f,
                viewH * 0.07f, WHITE, Paint.Align.CENTER, true);
        for (int i = 0; i < ALL_BLOCKS.length; i++) {
            RectF r = invTiles[i];
            int b = ALL_BLOCKS[i];
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF3A3F45);
            c.drawRoundRect(r, 14, 14, paint);
            float sx0 = r.left + r.width() * 0.12f, sx1 = r.right - r.width() * 0.12f;
            float sy0 = r.top + r.height() * 0.12f, sy1 = r.top + r.height() * 0.7f;
            
            if (texBlock[b] != null) {
                Bitmap tex = texBlock[b];
                if (b == GRASS) tex = texGrassSide;
                c.drawBitmap(tex, null, new RectF(sx0, sy0, sx1, sy1), paint);
            } else {
                paint.setColor(COLORS[b]);
                c.drawRoundRect(sx0, sy0, sx1, sy1, 10, 10, paint);
            }
            
            text(c, NAMES[b], r.centerX(), r.bottom - r.height() * 0.1f, r.height() * 0.16f,
                    WHITE, Paint.Align.CENTER, false);
            if (hotbar[sel] == b) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5f);
                paint.setColor(0xFFFFE066);
                c.drawRoundRect(r, 14, 14, paint);
            }
        }
        drawBox(c, invClose, "TUTUP", 0xFF7A7A7A);
    }

    void drawBox(Canvas c, RectF r, String label, int fill) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        c.drawRoundRect(r, r.height() * 0.15f, r.height() * 0.15f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(0xFF000000);
        c.drawRoundRect(r, r.height() * 0.15f, r.height() * 0.15f, paint);
        text(c, label, r.centerX(), r.centerY() + r.height() * 0.13f, r.height() * 0.35f,
                WHITE, Paint.Align.CENTER, true);
    }

    void layout(int w, int h) {
        viewW = w;
        viewH = h;
        btnR = h * 0.09f;
        
        dpadR = h * 0.18f;
        dpadX = dpadR * 1.5f;
        dpadY = h - dpadR * 1.5f;

        btnX[0] = w - btnR * 1.6f; btnY[0] = h - btnR * 1.6f;
        btnX[1] = w - btnR * 4.0f; btnY[1] = h - btnR * 1.6f;
        btnX[2] = w - btnR * 1.6f; btnY[2] = h - btnR * 4.0f;
        btnX[3] = w - btnR * 4.0f; btnY[3] = h - btnR * 4.0f;

        hotSlot = h * 0.1f;
        hotGap = h * 0.015f;
        float total = hotSlot * 9 + hotGap * 8;
        hotX0 = w / 2f - total / 2f;
        hotY0 = h - hotSlot - h * 0.03f;

        chatBtn.set(w / 2f - h * 0.15f, h * 0.03f, w / 2f + h * 0.15f, h * 0.13f);
        exitBtn.set(w - h * 0.3f, h * 0.03f, w - h * 0.03f, h * 0.13f);

        float tw = h * 0.3f, th = h * 0.2f, g = h * 0.03f;
        float tot = 5 * tw + 4 * g;
        float sx = w / 2f - tot / 2f, sy2 = h * 0.2f;
        for (int i = 0; i < 9; i++) {
            int col = i % 5, row = i / 5;
            float l = sx + col * (tw + g), t = sy2 + row * (th + g);
            invTiles[i].set(l, t, l + tw, t + th);
        }
        invClose.set(w / 2f - h * 0.14f, h * 0.78f, w / 2f + h * 0.14f, h * 0.88f);
    }

    int buttonAt(float x, float y) {
        for (int i = 0; i < 4; i++) {
            float dx = x - btnX[i], dy = y - btnY[i];
            float expandedR = btnR * 1.3f;
            if (dx * dx + dy * dy <= expandedR * expandedR) return i;
        }
        return -1;
    }

    int hotbarAt(float x, float y) {
        if (y < hotY0 || y > hotY0 + hotSlot) return -1;
        for (int i = 0; i < 9; i++) {
            float l = hotX0 + i * (hotSlot + hotGap);
            if (x >= l && x <= l + hotSlot) return i;
        }
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        synchronized (lock) {
            handleTouch(e);
        }
        return true;
    }

    void handleTouch(MotionEvent e) {
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int i = e.getActionIndex();
            int id = e.getPointerId(i) & 15;
            float x = e.getX(i), y = e.getY(i);

            tx[id] = x; ty[id] = y;

            if (invOpen) { inventoryTouch(x, y); return; }
            if (chatBtn.contains(x, y)) { openChat(); return; }
            if (exitBtn.contains(x, y)) { exitToMenu(); return; }

            int b = buttonAt(x, y);
            if (b >= 0) { role[id] = R_BTN; press(b); return; }
            int hs = hotbarAt(x, y);
            if (hs >= 0) { sel = hs; role[id] = R_HOT; return; }

            if (x < viewW * 0.45f) {
                role[id] = R_JOY;
                joyActive = true;
                joyX = clampF((x - dpadX) / dpadR);
                joyY = clampF((y - dpadY) / dpadR);
            } else {
                role[id] = R_LOOK;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (invOpen) return;
            for (int i = 0; i < e.getPointerCount(); i++) {
                int id = e.getPointerId(i) & 15;
                float x = e.getX(i), y = e.getY(i);
                if (role[id] == R_JOY) {
                    joyX = clampF((x - dpadX) / dpadR);
                    joyY = clampF((y - dpadY) / dpadR);
                } else if (role[id] == R_LOOK) {
                    yaw += (x - tx[id]) * lookSens;
                    pitch -= (y - ty[id]) * lookSens;
                    pitch = Math.max(-1.45, Math.min(1.45, pitch));
                    tx[id] = x; ty[id] = y;
                }
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP
                || action == MotionEvent.ACTION_CANCEL) {
            int n = (action == MotionEvent.ACTION_CANCEL) ? e.getPointerCount() : 1;
            for (int k = 0; k < n; k++) {
                int idx = (action == MotionEvent.ACTION_CANCEL) ? k : e.getActionIndex();
                int id = e.getPointerId(idx) & 15;
                if (role[id] == R_JOY) {
                    joyActive = false;
                    joyX = 0; joyY = 0;
                }
                role[id] = R_NONE;
            }
        }
    }

    void inventoryTouch(float x, float y) {
        for (int i = 0; i < ALL_BLOCKS.length; i++) {
            if (invTiles[i].contains(x, y)) {
                hotbar[sel] = ALL_BLOCKS[i];
                invOpen = false;
                return;
            }
        }
        if (invClose.contains(x, y)) invOpen = false;
    }

    void exitToMenu() {
        if (owner instanceof MainActivity) ((MainActivity) owner).backToMenu();
    }

    void openChat() {
        for (int i = 0; i < 16; i++) role[i] = R_NONE;
        joyActive = false;
        joyX = 0; joyY = 0;
        owner.runOnUiThread(new Runnable() {
            public void run() {
                final EditText input = new EditText(owner);
                input.setHint("/set waktu siang");
                input.setSingleLine(true);
                new AlertDialog.Builder(owner)
                        .setTitle("Chat / Command")
                        .setView(input)
                        .setPositiveButton("KIRIM", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                runCommand(input.getText().toString().trim());
                            }
                        })
                        .setNegativeButton("BATAL", null)
                        .show();
            }
        });
    }

    void showNotice(String msg) {
        notice = msg;
        noticeUntil = System.currentTimeMillis() + 3000;
    }

    void runCommand(String s) {
        if (s.length() == 0) return;
        if (!s.startsWith("/")) {
            showNotice("Command harus diawali /  contoh: /set waktu siang");
            return;
        }
        String[] p = s.substring(1).trim().toLowerCase().split("\\s+");
        if (p.length >= 3 && p[0].equals("set") && p[1].equals("waktu")) {
            String t = p[2];
            if (t.equals("pagi")) { dayT = 0.0; showNotice("Waktu: pagi"); }
            else if (t.equals("siang")) { dayT = 0.25; showNotice("Waktu: siang"); }
            else if (t.equals("sore")) { dayT = 0.42; showNotice("Waktu: sore"); }
            else if (t.equals("malam")) { dayT = 0.75; showNotice("Waktu: malam"); }
            else showNotice("Waktu: pagi / siang / sore / malam");
        } else if (p.length >= 3 && p[0].equals("set") && p[1].equals("cuaca")) {
            if (p[2].equals("hujan")) { raining = true; showNotice("Cuaca: hujan"); }
            else if (p[2].equals("terang") || p[2].equals("cerah")) { raining = false; showNotice("Cuaca: terang"); }
            else showNotice("Cuaca: hujan / terang");
        } else if (p[0].equals("help")) {
            showNotice("/set waktu pagi|siang|sore|malam   /set cuaca hujan|terang");
        } else {
            showNotice("Command tidak dikenal. Ketik /help");
        }
    }

    void update() {
        updatePlayer();
        updateAnimals();
        updateCamera();
        updateParticles();
        updateBlocks();
        dayT = (dayT + 1.0 / (30.0 * DAY_SECONDS)) % 1.0;
        rainLevel += ((raining ? 1f : 0f) - rainLevel) * 0.02f;
        cloudOff += 0.01;
        rainT += 1;
    }

    @Override
    public void run() {
        while (running) {
            long t0 = System.currentTimeMillis();
            if (viewW == 0) {
                try { Thread.sleep(16); } catch (InterruptedException e) { }
                continue;
            }
            Canvas c = null;
            synchronized (lock) {
                update();
                try {
                    c = getHolder().lockCanvas();
                    if (c != null) drawFrame(c);
                } finally {
                    if (c != null) getHolder().unlockCanvasAndPost(c);
                }
            }
            long dt = System.currentTimeMillis() - t0;
            if (dt < 33) {
                try { Thread.sleep(33 - dt); } catch (InterruptedException e) { }
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        synchronized (lock) {
            layout(w, h);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        try {
            if (thread != null) thread.join();
        } catch (InterruptedException e) { }
    }
}