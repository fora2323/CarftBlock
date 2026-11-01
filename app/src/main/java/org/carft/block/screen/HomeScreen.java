package org.carft.block.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
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

    private ModelBatch modelBatch;
    private Environment environment;
    private PerspectiveCamera camera3D;
    private VoxelArena arena;
    private VoxelRenderer renderer;
    private float camAngle = 0f;

    private Table mainTable;
    private Table settingsTable;
    private final Preferences prefs;

    public HomeScreen(CarftBlockGame game) {
        this.game = game;
        this.prefs = Gdx.app.getPreferences("carftblock");
        setup3DBackground();
        setupUI();
    }

    private void setup3DBackground() {
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.45f, 0.45f, 0.45f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        camera3D = new PerspectiveCamera(70, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera3D.near = 0.1f;
        camera3D.far = 300f;

        arena = new VoxelArena();
        renderer = new VoxelRenderer(arena);
    }

    private void setupUI() {
        stage = new Stage(new ExtendViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        Label titleLabel = new Label("CarftBlock PvP", game.uiAssets.skin, "title");
        titleLabel.setFontScale(2.8f);

        TextButton btnPlay = new TextButton("PLAY ARENA", game.uiAssets.skin);
        TextButton btnSettings = new TextButton("SETTINGS", game.uiAssets.skin);

        mainTable.add(titleLabel).padBottom(60).row();
        mainTable.add(btnPlay).width(340).height(85).padBottom(24).row();
        mainTable.add(btnSettings).width(340).height(85);

        settingsTable = new Table();
        settingsTable.setFillParent(true);
        settingsTable.setVisible(false);
        stage.addActor(settingsTable);

        Table panel = new Table(game.uiAssets.skin);
        panel.setBackground("panel-dark");
        panel.pad(40);

        final Label lblSens = new Label("Sensitivitas Kamera: " + String.format("%.1f", prefs.getFloat("sensitivity", 1.0f)), game.uiAssets.skin);
        final Slider sensSlider = new Slider(0.2f, 2.5f, 0.1f, false, game.uiAssets.skin);
        sensSlider.setValue(prefs.getFloat("sensitivity", 1.0f));

        TextButton btnBack = new TextButton("SIMPAN & KEMBALI", game.uiAssets.skin);

        panel.add(lblSens).padBottom(20).row();
        panel.add(sensSlider).width(420).height(40).padBottom(40).row();
        panel.add(btnBack).width(320).height(75);
        settingsTable.add(panel);

        sensSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                lblSens.setText("Sensitivitas Kamera: " + String.format("%.1f", sensSlider.getValue()));
            }
        });

        btnPlay.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game));
                dispose();
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
                prefs.flush();
                settingsTable.setVisible(false);
                mainTable.setVisible(true);
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.45f, 0.72f, 0.98f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        camAngle += delta * 12f;
        camera3D.position.set(
                32f + (float) Math.sin(Math.toRadians(camAngle)) * 24f,
                14f,
                32f + (float) Math.cos(Math.toRadians(camAngle)) * 24f
        );
        camera3D.lookAt(32f, 4f, 32f);
        camera3D.update();

        modelBatch.begin(camera3D);
        renderer.render(modelBatch, environment);
        modelBatch.end();

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
    public void dispose() {
        stage.dispose();
        modelBatch.dispose();
        renderer.dispose();
    }
}