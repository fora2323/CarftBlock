package org.carft.block.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import org.carft.block.world.VoxelArena;
import org.carft.block.world.VoxelRenderer;

public class CarftBlockGame extends ApplicationAdapter {
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    
    private VoxelArena arena;
    private VoxelRenderer renderer;

    private float camAngle = 0f;

    @Override
    public void create () {
        modelBatch = new ModelBatch();

        // Setup pencahayaan dasar agar dunia 3D tidak terlihat datar
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        camera = new PerspectiveCamera(75, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 300f;
        camera.update();

        arena = new VoxelArena();
        renderer = new VoxelRenderer(arena);
    }

    @Override
    public void render () {
        // Membersihkan layar dengan warna langit (Sky Blue)
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.5f, 0.8f, 1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Putar kamera sementara untuk mengetes render sebelum ada kontrol sentuh
        camAngle += Gdx.graphics.getDeltaTime() * 15f;
        camera.position.set(
            32f + (float)Math.sin(Math.toRadians(camAngle)) * 25f, 
            15f, 
            32f + (float)Math.cos(Math.toRadians(camAngle)) * 25f
        );
        camera.lookAt(32f, 5f, 32f);
        camera.update();

        modelBatch.begin(camera);
        renderer.render(modelBatch, environment);
        modelBatch.end();
    }

    @Override
    public void dispose () {
        modelBatch.dispose();
        renderer.dispose();
    }
}