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
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
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

    // SHADOW SYSTEM
    private DirectionalShadowLight shadowLight;
    private ModelBatch shadowBatch;

    private VoxelArena arena;
    private VoxelRenderer renderer;
    private Hud hud;

    private Model sharedMaleModel;
    private CharacterModel.Animator playerAnimator;

    private final Vector3 pos = new Vector3(32f, 1f, 28f);
    private final Vector3 vel = new Vector3();
    private final Vector3 diveVel = new Vector3();
    private final Vector3 camForward = new Vector3();
    private final Vector3 camRight = new Vector3(); // Untuk TPP Over The Shoulder
    private final Vector3 rayPos = new Vector3();

    // Variable mechanic
    private boolean isSliding = false;
    private boolean isClimbing = false;
    private float slideTimer = 0f;
    private final Vector3 slideDir = new Vector3();

    private float yaw = 0f;
    private float pitch = -10f;
    private boolean onGround = true;
    private int stance = CharacterModel.STANCE_STAND;
    private boolean isDolphinDiving = false;
    private boolean thirdPersonView = true;

    private float currentEyeHeight = 1.62f;
    private float targetFov = 75f;
    private boolean scopeActive = false;
    private float baseSens;

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
        hud.setHealth(75);
        hud.setArmor(100);
        updateWeaponHudText();

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(hud.stage);
        multiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(multiplexer);
    }

    private void setup3DWorld() {
        modelBatch = new ModelBatch();
        shadowBatch = new ModelBatch(new DepthShaderProvider());

        environment = new Environment();
        // Pencahayaan ambient dinaikkan sedikit agar bayangan tidak gelap gulita
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.65f, 1f));

        // Inisiasi Shadow Light (2048x2048 agar bayangan tajam)
        shadowLight = new DirectionalShadowLight(2048, 2048, 80f, 80f, 1f, 300f);
        shadowLight.set(0.7f, 0.7f, 0.7f, -0.6f, -0.9f, -0.4f);
        environment.add(shadowLight);
        environment.shadowMap = shadowLight;

        camera = new PerspectiveCamera(75f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 300f;

        arena = new VoxelArena();
        renderer = new VoxelRenderer(arena);

        sharedMaleModel = CharacterModel.createMaleModel();
        playerAnimator = new CharacterModel.Animator(sharedMaleModel);
    }

    private void findSafeSpawnPoint() {
        for (int z = 26; z < 40; z++) {
            if (!arena.isSolid(32, 1, z) && !arena.isSolid(32, 2, z)) {
                pos.set(32.5f, 1.0f, z + 0.5f);
                break;
            }
        }
    }

    @Override
    public void onJumpPressed() {
        if (onGround) {
            stance = CharacterModel.STANCE_STAND;
            isDolphinDiving = false;
            
            if (isSliding) {
                isSliding = false;
                vel.y = 7.0f;
            } else {
                vel.y = 8.2f;
            }
            
            hud.syncStanceButtons(stance);
            onGround = false;
        } else if (isClimbing) {
            vel.y = 7.0f;
            isClimbing = false;
        }
    }

    @Override
    public void onCrouchToggled() {
        if (isDolphinDiving || isClimbing) return;

        float padMag = (float) Math.hypot(hud.touchpad.getKnobPercentX(), hud.touchpad.getKnobPercentY());

        if (stance == CharacterModel.STANCE_STAND && padMag > 0.7f && onGround) {
            isSliding = true;
            slideTimer = 0.85f;
            stance = CharacterModel.STANCE_CROUCH;
            hud.syncStanceButtons(stance);

            float rad = (float) Math.toRadians(yaw);
            slideDir.set((float) Math.sin(rad), 0f, (float) Math.cos(rad)).nor();
            return;
        }

        if (isSliding) {
            isSliding = false;
            stance = CharacterModel.STANCE_STAND;
            hud.syncStanceButtons(stance);
            return;
        }

        stance = (stance == CharacterModel.STANCE_CROUCH)
                ? CharacterModel.STANCE_STAND
                : CharacterModel.STANCE_CROUCH;
        hud.syncStanceButtons(stance);
    }

    @Override
    public void onProneToggled() {
        if (isClimbing) return;

        float padMag = (float) Math.hypot(hud.touchpad.getKnobPercentX(), hud.touchpad.getKnobPercentY());

        if (!onGround && padMag > 0.45f && !isDolphinDiving) {
            isDolphinDiving = true;
            stance = CharacterModel.STANCE_PRONE;
            hud.syncStanceButtons(stance);

            float rad = (float) Math.toRadians(yaw);
            diveVel.set((float) Math.sin(rad) * 8.5f, 0f, (float) Math.cos(rad) * 8.5f);
            if (vel.y < 3.0f) vel.y = 3.5f;
            return;
        }

        isSliding = false; 

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

    private void updateGameplay(float delta) {
        float maxSpeed = (stance == CharacterModel.STANCE_STAND) ? 6.5f
                : (stance == CharacterModel.STANCE_CROUCH ? 3.2f : 1.6f);

        float joyX = hud.touchpad.getKnobPercentX();
        float joyY = hud.touchpad.getKnobPercentY();
        float joyMag = Math.min(1f, (float) Math.hypot(joyX, joyY));

        float rad = (float) Math.toRadians(yaw);
        float sin = (float) Math.sin(rad);
        float cos = (float) Math.cos(rad);

        float playerHeight = (stance == CharacterModel.STANCE_STAND) ? 1.75f
                : (stance == CharacterModel.STANCE_CROUCH ? 1.25f : 0.65f);

        if (!onGround && !isSliding && !isDolphinDiving) {
            float fwdX = sin * 0.4f;
            float fwdZ = cos * 0.4f;
            boolean wallInFront = collidesWithArena(pos.x + fwdX, pos.y, pos.z + fwdZ, playerHeight);
            
            if (wallInFront && joyY > 0.5f) {
                isClimbing = true;
                vel.y = 4.2f; 
                vel.x = 0f;
                vel.z = 0f;
            } else {
                isClimbing = false;
            }
        } else {
            isClimbing = false;
        }

        if (isClimbing) {
            // Bergerak ke atas
        } else if (isDolphinDiving && !onGround) {
            vel.x = diveVel.x;
            vel.z = diveVel.z;
        } else if (isSliding) {
            slideTimer -= delta;
            float slideSpeed = 3.5f + 6.5f * (slideTimer / 0.85f);
            vel.x = slideDir.x * slideSpeed;
            vel.z = slideDir.z * slideSpeed;
            if (slideTimer <= 0f) isSliding = false; 
        } else {
            vel.x = (sin * joyY - cos * joyX) * maxSpeed;
            vel.z = (cos * joyY + sin * joyX) * maxSpeed;
        }

        if (!isClimbing) {
            vel.y -= 22f * delta;
            if (vel.y < -25f) vel.y = -25f;
        }

        float nextX = pos.x + vel.x * delta;
        if (!collidesWithArena(nextX, pos.y, pos.z, playerHeight)) {
            pos.x = nextX;
        } else if (isSliding) {
            isSliding = false; 
        }

        float nextZ = pos.z + vel.z * delta;
        if (!collidesWithArena(pos.x, pos.y, nextZ, playerHeight)) {
            pos.z = nextZ;
        } else if (isSliding) {
            isSliding = false;
        }

        float nextY = pos.y + vel.y * delta;
        if (vel.y <= 0f) {
            if (collidesWithArena(pos.x, nextY, pos.z, playerHeight)) {
                pos.y = (float) Math.floor(nextY) + 1.0f;
                vel.y = 0f;
                onGround = true;
                if (isDolphinDiving) {
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
                isClimbing = false; 
            } else {
                pos.y = nextY;
                onGround = false;
            }
        }

        playerAnimator.update(delta, pos, yaw, pitch, joyMag, onGround, stance, isDolphinDiving, isSliding, isClimbing);

        float targetEye = (stance == CharacterModel.STANCE_STAND) ? 1.62f
                : (stance == CharacterModel.STANCE_CROUCH ? (isSliding ? 0.75f : 1.12f) : 0.42f);
        
        currentEyeHeight = MathUtils.lerp(currentEyeHeight, targetEye, Math.min(1f, delta * 12f));
        camera.fieldOfView = MathUtils.lerp(camera.fieldOfView, targetFov, Math.min(1f, delta * 12f));

        float pitchRad = (float) Math.toRadians(pitch);
        camForward.set(
                sin * (float) Math.cos(pitchRad),
                (float) Math.sin(pitchRad),
                cos * (float) Math.cos(pitchRad)
        ).nor();

        // Cari vector Kanan untuk kamera Over The Shoulder
        camRight.set(camForward).crs(Vector3.Y).nor();

        if (thirdPersonView && !scopeActive) {
            float camDist = 3.6f;
            float otsOffset = 0.65f; // Geser kamera ke Kanan (Bahu Karakter)

            float targetCamX = pos.x + camRight.x * otsOffset;
            float targetCamY = pos.y + currentEyeHeight;
            float targetCamZ = pos.z + camRight.z * otsOffset;

            for (float d = 0.5f; d <= 3.6f; d += 0.4f) {
                float cx = targetCamX - camForward.x * d;
                float cy = targetCamY - camForward.y * d;
                float cz = targetCamZ - camForward.z * d;
                if (arena.isSolid((int) Math.floor(cx), (int) Math.floor(cy), (int) Math.floor(cz))) {
                    camDist = Math.max(0.6f, d - 0.5f);
                    break;
                }
            }
            
            camera.position.set(
                    targetCamX - camForward.x * camDist,
                    targetCamY - camForward.y * camDist + 0.25f,
                    targetCamZ - camForward.z * camDist
            );
        } else {
            camera.position.set(pos.x, pos.y + currentEyeHeight, pos.z);
        }

        camera.direction.set(camForward);
        camera.up.set(0f, 1f, 0f);
        camera.update();

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
        // BUG AKURASI CROSSHAIR DIPERBAIKI: 
        // Titik tembak tembak kini berasal tepat dari kamera menuju tengah layar (Crosshair)
        // Jadi mode FPS / TPP arah tembakan tetap 100% akurat mengenai bidikan
        rayPos.set(camera.position); 
        for (float t = 0f; t < 60f; t += 0.25f) {
            int bx = (int) Math.floor(rayPos.x + camera.direction.x * t);
            int by = (int) Math.floor(rayPos.y + camera.direction.y * t);
            int bz = (int) Math.floor(rayPos.z + camera.direction.z * t);
            if (arena.isSolid(bx, by, bz)) {
                break; // Tembakan mengenai arena
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

        // --- MULAI RENDER BAYANGAN (SHADOW MAP) ---
        // Target lampu difokuskan ke tengah arena (32, 0, 32)
        shadowLight.begin(new Vector3(32f, 0f, 32f), shadowLight.direction);
        shadowBatch.begin(shadowLight.getCamera());
        renderer.render(shadowBatch, null); // Render Arena ke bayangan
        if (thirdPersonView && !scopeActive) {
            shadowBatch.render(playerAnimator.instance); // Render Player ke bayangan
        }
        shadowBatch.end();
        shadowLight.end();
        // --- SELESAI RENDER BAYANGAN ---

        // --- RENDER DUNIA 3D NORMAL ---
        modelBatch.begin(camera);
        renderer.render(modelBatch, environment); // Arena menerima cahaya & bayangan
        if (thirdPersonView && !scopeActive) {
            modelBatch.render(playerAnimator.instance, environment); // Player menerima cahaya & bayangan
        }
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
        shadowBatch.dispose();
        shadowLight.dispose();
        renderer.dispose();
        hud.dispose();
        sharedMaleModel.dispose();
    }

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