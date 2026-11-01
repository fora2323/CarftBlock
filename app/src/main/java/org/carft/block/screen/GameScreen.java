package org.carft.block.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;

import org.carft.block.game.CarftBlockGame;
import org.carft.block.entity.CharacterModel;
import org.carft.block.ui.Hud;
import org.carft.block.world.VoxelArena;
import org.carft.block.world.VoxelRenderer;

public class GameScreen extends ScreenAdapter implements InputProcessor {
    private final CarftBlockGame game;
    private Preferences prefs;

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;

    private VoxelArena arena;
    private VoxelRenderer renderer;
    private Hud hud;

    // Model Karakter Uji Coba
    private Model playerModelBase;
    private ModelInstance dummyPlayer;

    // Posisi & Fisika Kamera Pemain
    private Vector3 pos = new Vector3(32f, 15f, 32f); // Titik Spawn (Tengah atas)
    private Vector3 vel = new Vector3();
    private float yaw = 0f, pitch = 0f;
    private boolean isJumping = false;
    private float lookSens;

    public GameScreen(CarftBlockGame game) {
        this.game = game;
        this.prefs = Gdx.app.getPreferences("carftblock");
        this.lookSens = prefs.getFloat("sensitivity", 1.0f) * 0.2f;

        // 1. Setup 3D Environment
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.5f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        camera = new PerspectiveCamera(75, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 300f;

        arena = new VoxelArena();
        renderer = new VoxelRenderer(arena);

        // 2. Setup Dummy Character Model di tengah arena
        playerModelBase = CharacterModel.createMale();
        dummyPlayer = new ModelInstance(playerModelBase);
        dummyPlayer.transform.setToTranslation(32f, 2f, 32f); // Berdiri di atas lantai

        // 3. Setup HUD & Input
        hud = new Hud();
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(hud.stage); // Stage dieksekusi lebih dulu (tombol)
        multiplexer.addProcessor(this);      // Screen dieksekusi kedua (swipe kamera)
        Gdx.input.setInputProcessor(multiplexer);

        // Event Tombol HUD
        hud.btnSettings.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.setScreen(new HomeScreen(game)); // Exit sementara kembali ke Home
            }
        });
        
        hud.btnJump.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                if (!isJumping) {
                    vel.y = 8f; // Tenaga lompat
                    isJumping = true;
                }
            }
        });
    }

    private void updatePhysics(float delta) {
        // Gerakan dari Joystick Kiri
        float speed = 5f;
        float moveX = hud.touchpad.getKnobPercentX();
        float moveY = -hud.touchpad.getKnobPercentY(); // Y dibalik agar maju ke depan

        // Menghitung vektor maju berdasarkan arah kamera (Yaw)
        float sin = (float) Math.sin(Math.toRadians(yaw));
        float cos = (float) Math.cos(Math.toRadians(yaw));
        
        vel.x = (cos * moveY - sin * moveX) * speed;
        vel.z = (sin * moveY + cos * moveX) * speed;
        
        // Gravitasi
        vel.y -= 20f * delta; 

        // Update Posisi Sementara
        float nextX = pos.x + vel.x * delta;
        float nextY = pos.y + vel.y * delta;
        float nextZ = pos.z + vel.z * delta;

        // Cek Kolisi AABB sangat sederhana (titik pusat kaki pemain)
        if (arena.isSolid((int)nextX, (int)nextY, (int)pos.z)) {
            vel.x = 0; nextX = pos.x;
        }
        if (arena.isSolid((int)pos.x, (int)nextY, (int)nextZ)) {
            vel.z = 0; nextZ = pos.z;
        }
        if (arena.isSolid((int)pos.x, (int)nextY, (int)pos.z)) {
            if (vel.y < 0) isJumping = false;
            vel.y = 0; nextY = (float)Math.ceil(pos.y); 
        }

        pos.set(nextX, nextY, nextZ);

        // Update Kamera (Tinggi mata = Y + 1.5)
        camera.position.set(pos.x, pos.y + 1.5f, pos.z);
        camera.direction.set(0, 0, 1);
        camera.up.set(0, 1, 0);
        camera.rotate(camera.up, yaw);
        camera.rotate(camera.direction.cpy().crs(camera.up), pitch);
        camera.update();
    }

    @Override
    public void render(float delta) {
        updatePhysics(delta);
        hud.update(delta);

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.5f, 0.8f, 1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        renderer.render(modelBatch, environment); // Render Dunia
        modelBatch.render(dummyPlayer, environment); // Render Karakter
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
        playerModelBase.dispose();
    }

    // --- KONTROL SENTUH KAMERA (KANAN) ---
    private int lookPointer = -1;
    private float lastTouchX, lastTouchY;

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // Jika jari menyentuh sisi kanan layar (bukan HUD), catat sebagai kontrol kamera
        if (screenX > Gdx.graphics.getWidth() / 2f) {
            lookPointer = pointer;
            lastTouchX = screenX;
            lastTouchY = screenY;
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (pointer == lookPointer) {
            float deltaX = screenX - lastTouchX;
            float deltaY = screenY - lastTouchY;
            
            yaw -= deltaX * lookSens;
            pitch -= deltaY * lookSens;
            
            if (pitch > 89f) pitch = 89f;
            if (pitch < -89f) pitch = -89f;

            lastTouchX = screenX;
            lastTouchY = screenY;
            return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (pointer == lookPointer) lookPointer = -1;
        return false;
    }
    
    // Method InputProcessor kosong lainnya
    @Override public boolean keyDown(int keycode) {return false; }
    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
}