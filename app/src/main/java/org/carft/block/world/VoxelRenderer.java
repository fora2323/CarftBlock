package org.carft.block.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

public class VoxelRenderer implements Disposable {
    private Model model;
    private ModelInstance instance;
    private Texture texture;

    public VoxelRenderer(VoxelArena arena) {
        createProceduralTexture();
        buildMesh(arena);
    }

    private void createProceduralTexture() {
        // Membuat tekstur 16x16 piksel secara prosedural agar tidak butuh file eksternal
        Pixmap pixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixmap.setColor(Color.LIGHT_GRAY);
        pixmap.drawRectangle(0, 0, 16, 16); // Garis tepi blok ala Minecraft
        
        texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest); // Matikan blur (Pixel art)
        pixmap.dispose();
    }

    private void buildMesh(VoxelArena arena) {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        Material material = new Material(TextureAttribute.createDiffuse(texture));
        
        // Memakai Vertex Color untuk membedakan tipe blok meski teksturnya sama
        MeshPartBuilder builder = modelBuilder.part("arena_mesh", GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal |
                VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorUnpacked,
                material);

        // Objek dipisah di luar loop agar tidak men-trigger Garbage Collector berulang
        Vector3 p000 = new Vector3(), p010 = new Vector3(), p100 = new Vector3(), p110 = new Vector3();
        Vector3 p001 = new Vector3(), p011 = new Vector3(), p101 = new Vector3(), p111 = new Vector3();
        Vector3 normal = new Vector3();

        for (int x = 0; x < arena.width; x++) {
            for (int y = 0; y < arena.height; y++) {
                for (int z = 0; z < arena.depth; z++) {
                    byte block = arena.getBlock(x, y, z);
                    if (block == VoxelArena.AIR) continue;

                    Color col = Color.WHITE;
                    if (block == VoxelArena.FLOOR) col = Color.DARK_GRAY;
                    if (block == VoxelArena.WALL) col = Color.BROWN;
                    if (block == VoxelArena.OBSTACLE) col = Color.OLIVE;
                    
                    builder.setColor(col);

                    p000.set(x, y, z);     p010.set(x, y+1, z);
                    p100.set(x+1, y, z);   p110.set(x+1, y+1, z);
                    p001.set(x, y, z+1);   p011.set(x, y+1, z+1);
                    p101.set(x+1, y, z+1); p111.set(x+1, y+1, z+1);

                    // ALGORITMA FACE CULLING: Sisi digambar HANYA jika bersebelahan dengan udara
                    if (!arena.isSolid(x, y+1, z)) { // Atas
                        normal.set(0, 1, 0);
                        builder.rect(p011, p111, p110, p010, normal);
                    }
                    if (!arena.isSolid(x, y-1, z)) { // Bawah
                        normal.set(0, -1, 0);
                        builder.rect(p000, p100, p101, p001, normal);
                    }
                    if (!arena.isSolid(x-1, y, z)) { // Kiri
                        normal.set(-1, 0, 0);
                        builder.rect(p000, p001, p011, p010, normal);
                    }
                    if (!arena.isSolid(x+1, y, z)) { // Kanan
                        normal.set(1, 0, 0);
                        builder.rect(p101, p100, p110, p111, normal);
                    }
                    if (!arena.isSolid(x, y, z-1)) { // Belakang
                        normal.set(0, 0, -1);
                        builder.rect(p100, p000, p010, p110, normal);
                    }
                    if (!arena.isSolid(x, y, z+1)) { // Depan
                        normal.set(0, 0, 1);
                        builder.rect(p001, p101, p111, p011, normal);
                    }
                }
            }
        }
        model = modelBuilder.end();
        instance = new ModelInstance(model);
    }

    public void render(ModelBatch batch, Environment env) {
        batch.render(instance, env);
    }

    @Override
    public void dispose() {
        if (model != null) model.dispose();
        if (texture != null) texture.dispose();
    }
}