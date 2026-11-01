package org.carft.block.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import org.carft.block.game.CarftBlockGame;
import org.carft.block.world.VoxelArena;
import org.carft.block.world.VoxelRenderer;

public class HomeScreen extends ScreenAdapter {
    private final CarftBlockGame game;
    private Stage stage;

    // Komponen Background 3D
    private ModelBatch modelBatch;
    private Environment environment;
    private PerspectiveCamera camera3D;
    private VoxelArena arena;
    private VoxelRenderer renderer;
    private float camAngle = 0f;

    // UI
    private Table mainTable;
    private Table settingsTable;
    private Preferences prefs;

    public HomeScreen(CarftBlockGame game) {
        this.game = game;
        this.prefs = Gdx.app.getPreferences("carftblock");
        
        setup3DBackground();
        setupUI();
    }

    private void setup3DBackground() {
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        camera3D = new PerspectiveCamera(75, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera3D.near = 0.1f;
        camera3D.far = 300f;

        arena = new VoxelArena();
        renderer = new VoxelRenderer(arena);
    }

    private void setupUI() {
        // Resolusi virtual dasar 1280x720, otomatis diperluas jika rasio layar berbeda
        stage = new Stage(new ExtendViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        // Setup Tabel Menu Utama
        mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        Label.LabelStyle labelStyle = new Label.LabelStyle(game.uiAssets.skin.getFont("default-font"), Color.YELLOW);
        Label titleLabel = new Label("CARFTBLOCK", labelStyle);
        titleLabel.setFontScale(3f);

        TextButton btnPlay = new TextButton("PLAY", game.uiAssets.skin);
        TextButton btnSettings = new TextButton("SETTINGS", game.uiAssets.skin);

        mainTable.add(titleLabel).padBottom(80).row();
        mainTable.add(btnPlay).width(300).height(80).padBottom(20).row();
        mainTable.add(btnSettings).width(300).height(80);

        // Setup Tabel Pengaturan (Awalnya disembunyikan)
        settingsTable = new Table();
        settingsTable.setFillParent(true);
        settingsTable.setVisible(false);
        stage.addActor(settingsTable);

        Label lblSens = new Label("Sensitivitas Kamera", labelStyle);
        
        // Slider nilai 0.1 hingga 2.0. Nilai default 1.0.
        final Slider sensSlider = new Slider(0.1f, 2.0f, 0.1f, false, game.uiAssets.skin);
        sensSlider.setValue(prefs.getFloat("sensitivity", 1.0f));
        
        TextButton btnBack = new TextButton("KEMBALI", game.uiAssets.skin);

        settingsTable.add(lblSens).padBottom(20).row();
        settingsTable.add(sensSlider).width(400).padBottom(60).row();
        settingsTable.add(btnBack).width(300).height(80);

        btnPlay.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game));
            }
        });

        btnSettings.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                mainTable.setVisible(false);
                settingsTable.setVisible(true);
            }
        });

        btnBack.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                prefs.putFloat("sensitivity", sensSlider.getValue());
                prefs.flush(); // Simpan permanen
                settingsTable.setVisible(false);
                mainTable.setVisible(true);
            }
        });
    }

    @Override
    public void render(float delta) {
        // Membersihkan layar
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.3f, 0.6f, 0.9f, 1f); // Warna langit
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Putar kamera untuk background 3D
        camAngle += delta * 10f;
        camera3D.position.set(32f + (float)Math.sin(Math.toRadians(camAngle)) * 25f, 15f, 32f + (float)Math.cos(Math.toRadians(camAngle)) * 25f);
        camera3D.lookAt(32f, 5f, 32f);
        camera3D.update();

        // Render Background 3D
        modelBatch.begin(camera3D);
        renderer.render(modelBatch, environment);
        modelBatch.end();

        // Render UI di atas background 3D
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        camera3D.viewportWidth = width;
        camera3D.viewportHeight = height;
        camera3D.update();
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
        modelBatch.dispose();
        renderer.dispose();
    }
}