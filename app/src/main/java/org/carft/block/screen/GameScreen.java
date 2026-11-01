package org.carft.block.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import org.carft.block.entity.CharacterModel;
import org.carft.block.game.CarftBlockGame;
import org.carft.block.ui.Hud;
import org.carft.block.world.VoxelArena;
import org.carft.block.world.VoxelRenderer;

public class GameScreen extends ScreenAdapter implements InputProcessor, Hud.HudListener {
    private final CarftBlockGame game;
    private final Preferences prefs;

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;

    private VoxelArena arena;
    private VoxelRenderer renderer;
    private Hud hud;

    // Model & Animator Karakter Pemain dan Bot Dummy
    private Model sharedMaleModel;
    private CharacterModel.Animator playerAnimator;
    private CharacterModel.Animator dummyAnimator;
    private final Vector3 dummyPos = new Vector3(32f, 1f, 37f);

    // Fisika & Status Pemain (Tanpa alokasi objek baru di render)
    private final Vector3 pos = new Vector3(32f, 1f, 28f);
    private final Vector3 vel = new Vector3();
    private final Vector3 diveVel = new Vector3();
    private final Vector3 camForward = new Vector3();
    private final Vector3 rayPos = new Vector3();

    private float yaw = 0f;
    private float pitch = -10f;
    private boolean onGround = true;
    private int stance = CharacterModel.STANCE_STAND;
    private boolean isDolphinDiving = false;
    private boolean thirdPersonView = true; // Default 3rd Person agar animasi langsung terlihat

    private float currentEyeHeight = 1.62f;
    private float targetFov = 75f;
    private boolean scopeActive = false;
    private float baseSens;

    // Data Senjata (0 = Rifle, 1 = Pistol)
    private int activeWeapon = 0;
    private final String[] weaponNames = {"RIFLE", "PISTOL"};
    private final int[] magMax = {30, 12};
    private final int[] ammoInMag = {30, 12};
    private final float[] fireIntervals = {0.12f, 0.28f};
    private float fireTimer = 0f;
    private float reloadTimer = 0f;

    private int lookPointer = -1;
    private float lastTouchX, lastTouchY;

    public GameScreen(CarftBlockGame game) {
        this.game = game;
        this.prefs = Gdx.app.getPreferences("carftblock");
        this.baseSens = prefs.getFloat("sensitivity", 1.0f) * 0.22f;

        setup3DWorld();
        findSafeSpawnPoint();

        hud = new Hud(game.uiAssets, arena, this);
        hud.setHealth(75); // Set 75 di awal agar tombol Medkit bisa langsung dites
        hud.setArmor(100);
        updateWeaponHudText();

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(hud.stage);
        multiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(multiplexer);
    }

    private void setup3DWorld() {
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.5f, 1f));
        environment.add(new DirectionalLight().set(0.85f, 0.85f, 0.85f, -0.6f, -0.9f, -0.4f));

        camera = new PerspectiveCamera(75f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 300f;

        arena = new VoxelArena();
        renderer = new VoxelRenderer(arena);

        sharedMaleModel = CharacterModel.createMaleModel();
        playerAnimator = new CharacterModel.Animator(sharedMaleModel);
        dummyAnimator = new CharacterModel.Animator(sharedMaleModel);
    }

    private void findSafeSpawnPoint() {
        // Cari koordinat dekat tengah arena yang tidak tertutup pilar
        for (int z = 26; z < 40; z++) {
            if (!arena.isSolid(32, 1, z) && !arena.isSolid(32, 2, z)) {
                pos.set(32.5f, 1.0f, z + 0.5f);
                break;
            }
        }
    }

    // --- IMPLEMENTASI AKSI HUD ---
    @Override
    public void onJumpPressed() {
        if (onGround) {
            // Loncat membatalkan jongkok/tiarap
            stance = CharacterModel.STANCE_STAND;
            isDolphinDiving = false;
            hud.syncStanceButtons(stance);
            vel.y = 8.2f;
            onGround = false;
        }
    }

    @Override
    public void onCrouchToggled() {
        if (isDolphinDiving) return;
        stance = (stance == CharacterModel.STANCE_CROUCH)
                ? CharacterModel.STANCE_STAND
                : CharacterModel.STANCE_CROUCH;
        hud.syncStanceButtons(stance);
    }

    @Override
    public void onProneToggled() {
        float padMag = (float) Math.hypot(hud.touchpad.getKnobPercentX(), hud.touchpad.getKnobPercentY());

        // FITUR SPESIAL: Saat Lari + Lompat (di udara) lalu klik TIARAP -> Lompat sambil Telentang!
        if (!onGround && padMag > 0.45f && !isDolphinDiving) {
            isDolphinDiving = true;
            stance = CharacterModel.STANCE_PRONE;
            hud.syncStanceButtons(stance);

            // Beri dorongan loncat ke depan di udara
            float rad = (float) Math.toRadians(yaw);
            diveVel.set((float) Math.sin(rad) * 8.5f, 0f, (float) Math.cos(rad) * 8.5f);
            if (vel.y < 3.0f) vel.y = 3.5f;
            return;
        }

        // Toggle Tiarap normal saat di darat
        stance = (stance == CharacterModel.STANCE_PRONE)
                ? CharacterModel.STANCE_STAND
                : CharacterModel.STANCE_PRONE;
        hud.syncStanceButtons(stance);
    }

    @Override
    public void onScopeToggled(boolean active) {
        this.scopeActive = active;
        this.targetFov = active ? 35f : 75f;
    }

    @Override
    public void onWeaponSelected(int index) {
        this.activeWeapon = index;
        this.reloadTimer = 0f;
        updateWeaponHudText();
    }

    @Override
    public void onGrenadeUsed() {
        hud.setWeaponStatus("GRANAT DILEMPAR!");
    }

    @Override
    public void onMedkitUsed() {
        hud.setWeaponStatus("+50 HP DIPULIHKAN");
    }

    @Override
    public void onCameraViewToggled() {
        thirdPersonView = !thirdPersonView;
        hud.setCameraButtonLabel(thirdPersonView);
    }

    @Override
    public void onExitToHome() {
        game.setScreen(new HomeScreen(game));
        dispose();
    }

    // --- FISIKA, KOLISI AABB & ANIMASI ---
    private void updateGameplay(float delta) {
        // 1. Kecepatan gerak sesuai postur (Berdiri / Jongkok / Tiarap)
        float maxSpeed = (stance == CharacterModel.STANCE_STAND) ? 6.5f
                : (stance == CharacterModel.STANCE_CROUCH ? 3.2f : 1.6f);

        float joyX = hud.touchpad.getKnobPercentX();
        float joyY = hud.touchpad.getKnobPercentY();
        float joyMag = Math.min(1f, (float) Math.hypot(joyX, joyY));

        float rad = (float) Math.toRadians(yaw);
        float sin = (float) Math.sin(rad);
        float cos = (float) Math.cos(rad);

        if (isDolphinDiving && !onGround) {
            // Saat lompat telentang di udara, pertahankan momentum loncatan
            vel.x = diveVel.x;
            vel.z = diveVel.z;
        } else {
            // Gerak maju/mundur & geser kanan/kiri sesuai arah hadap kamera
            vel.x = (sin * joyY - cos * joyX) * maxSpeed;
            vel.z = (cos * joyY + sin * joyX) * maxSpeed;
        }

        // 2. Gravitasi dan Perbaikan Bug Naik-Turun
        vel.y -= 22f * delta;
        if (vel.y < -25f) vel.y = -25f;

        float playerHeight = (stance == CharacterModel.STANCE_STAND) ? 1.75f
                : (stance == CharacterModel.STANCE_CROUCH ? 1.25f : 0.65f);

        // Resolusi Sumbu X
        float nextX = pos.x + vel.x * delta;
        if (!collidesWithArena(nextX, pos.y, pos.z, playerHeight)) {
            pos.x = nextX;
        }

        // Resolusi Sumbu Z
        float nextZ = pos.z + vel.z * delta;
        if (!collidesWithArena(pos.x, pos.y, nextZ, playerHeight)) {
            pos.z = nextZ;
        }

        // Resolusi Sumbu Y (Bebas bug naik-turun menggunakan Math.floor)
        float nextY = pos.y + vel.y * delta;
        if (vel.y <= 0f) {
            if (collidesWithArena(pos.x, nextY, pos.z, playerHeight)) {
                pos.y = (float) Math.floor(nextY) + 1.0f;
                vel.y = 0f;
                onGround = true;
                if (isDolphinDiving) {
                    // Begitu mendarat dari lompat telentang, otomatis masuk ke mode tiarap
                    isDolphinDiving = false;
                    stance = CharacterModel.STANCE_PRONE;
                    hud.syncStanceButtons(stance);
                }
            } else {
                pos.y = nextY;
                onGround = false;
            }
        } else {
            if (collidesWithArena(pos.x, nextY, pos.z, playerHeight)) {
                vel.y = 0f;
            } else {
                pos.y = nextY;
                onGround = false;
            }
        }

        // 3. Update Animasi Karakter Pemain & Karakter Dummy (Mirror)
        playerAnimator.update(delta, pos, yaw, pitch, joyMag, onGround, stance, isDolphinDiving);
        dummyAnimator.update(delta, dummyPos, yaw + 180f, pitch, joyMag, onGround, stance, isDolphinDiving);

        // 4. Transisi halus tinggi mata kamera & Zoom Scope
        float targetEye = (stance == CharacterModel.STANCE_STAND) ? 1.62f
                : (stance == CharacterModel.STANCE_CROUCH ? 1.12f : 0.42f);
        currentEyeHeight = MathUtils.lerp(currentEyeHeight, targetEye, Math.min(1f, delta * 12f));
        camera.fieldOfView = MathUtils.lerp(camera.fieldOfView, targetFov, Math.min(1f, delta * 12f));

        // 5. Hitung arah vektor kamera
        float pitchRad = (float) Math.toRadians(pitch);
        camForward.set(
                sin * (float) Math.cos(pitchRad),
                (float) Math.sin(pitchRad),
                cos * (float) Math.cos(pitchRad)
        ).nor();

        if (thirdPersonView && !scopeActive) {
            // Kamera 3rd Person berada di belakang bahu karakter (dengan pengecekan dinding)
            float camDist = 3.6f;
            for (float d = 0.5f; d <= 3.6f; d += 0.4f) {
                float cx = pos.x - camForward.x * d;
                float cy = pos.y + currentEyeHeight - camForward.y * d;
                float cz = pos.z - camForward.z * d;
                if (arena.isSolid((int) Math.floor(cx), (int) Math.floor(cy), (int) Math.floor(cz))) {
                    camDist = Math.max(0.6f, d - 0.5f);
                    break;
                }
            }
            camera.position.set(
                    pos.x - camForward.x * camDist,
                    pos.y + currentEyeHeight - camForward.y * camDist + 0.25f,
                    pos.z - camForward.z * camDist
            );
        } else {
            // Kamera 1st Person
            camera.position.set(pos.x, pos.y + currentEyeHeight, pos.z);
        }

        camera.direction.set(camForward);
        camera.up.set(0f, 1f, 0f);
        camera.update();

        // 6. Update Tembakan & Reload
        updateWeapons(delta);
        hud.refreshTopDownMap(pos.x, pos.z, yaw);
    }

    private boolean collidesWithArena(float x, float y, float z, float height) {
        float r = 0.30f;
        int minX = (int) Math.floor(x - r);
        int maxX = (int) Math.floor(x + r);
        int minY = (int) Math.floor(y);
        int maxY = (int) Math.floor(y + height - 0.05f);
        int minZ = (int) Math.floor(z - r);
        int maxZ = (int) Math.floor(z + r);

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (arena.isSolid(bx, by, bz)) return true;
                }
            }
        }
        return false;
    }

    private void updateWeapons(float delta) {
        if (fireTimer > 0f) fireTimer -= delta;

        if (reloadTimer > 0f) {
            reloadTimer -= delta;
            if (reloadTimer <= 0f) {
                ammoInMag[activeWeapon] = magMax[activeWeapon];
                updateWeaponHudText();
            }
            return;
        }

        if (hud.isFiringPressed() && fireTimer <= 0f) {
            if (ammoInMag[activeWeapon] > 0) {
                ammoInMag[activeWeapon]--;
                fireTimer = fireIntervals[activeWeapon];
                performShootRaycast();

                if (ammoInMag[activeWeapon] == 0) {
                    reloadTimer = 1.5f;
                    hud.setWeaponStatus(weaponNames[activeWeapon] + " | RELOADING...");
                } else {
                    updateWeaponHudText();
                }
            }
        }
    }

    private void performShootRaycast() {
        // Raycast dari mata pemain berhenti di blok pertama yang kena
        rayPos.set(pos.x, pos.y + currentEyeHeight, pos.z);
        for (float t = 0f; t < 60f; t += 0.25f) {
            int bx = (int) Math.floor(rayPos.x + camForward.x * t);
            int by = (int) Math.floor(rayPos.y + camForward.y * t);
            int bz = (int) Math.floor(rayPos.z + camForward.z * t);
            if (arena.isSolid(bx, by, bz)) {
                break;
            }
        }
    }

    private void updateWeaponHudText() {
        hud.setWeaponStatus(weaponNames[activeWeapon] + " | " + ammoInMag[activeWeapon] + "/" + magMax[activeWeapon]);
    }

    @Override
    public void render(float delta) {
        float safeDelta = Math.min(delta, 0.05f);
        if (!hud.isPaused()) {
            updateGameplay(safeDelta);
        }
        hud.update(safeDelta);

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.48f, 0.75f, 0.98f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        renderer.render(modelBatch, environment);
        // Render karakter pemain jika di mode 3rd Person (dan tidak sedang Scope)
        if (thirdPersonView && !scopeActive) {
            modelBatch.render(playerAnimator.instance, environment);
        }
        // Render karakter kedua di tengah arena yang menirukan gerakanmu
        modelBatch.render(dummyAnimator.instance, environment);
        modelBatch.end();

        hud.render();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        hud.stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        renderer.dispose();
        hud.dispose();
        sharedMaleModel.dispose();
    }

    // --- INPUT SWIPE KAMERA DI AREA KANAN LAYAR ---
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (hud.isOverlayOpen()) return false;
        if (screenX > Gdx.graphics.getWidth() * 0.42f && lookPointer == -1) {
            lookPointer = pointer;
            lastTouchX = screenX;
            lastTouchY = screenY;
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (pointer == lookPointer && !hud.isOverlayOpen()) {
            float sens = scopeActive ? baseSens * 0.45f : baseSens;
            float dx = screenX - lastTouchX;
            float dy = screenY - lastTouchY;

            yaw -= dx * sens;
            pitch -= dy * sens;
            pitch = MathUtils.clamp(pitch, -80f, 80f);

            lastTouchX = screenX;
            lastTouchY = screenY;
            return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (pointer == lookPointer) {
            lookPointer = -1;
            return true;
        }
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        if (pointer == lookPointer) {
            lookPointer = -1;
        }
        return false;
    }

    @Override public boolean keyDown(int keycode) { return false; }
    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }
}