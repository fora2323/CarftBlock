package org.carft.block.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

public class CharacterModel {

    // Membuat model 3D Karakter Pria gaya Voxel/Minecraft
    public static Model createMale() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        
        // Warna material (tanpa tekstur gambar)
        Material skin = new Material(ColorAttribute.createDiffuse(new Color(0.96f, 0.80f, 0.69f, 1f))); // Kulit
        Material shirt = new Material(ColorAttribute.createDiffuse(new Color(0.2f, 0.6f, 0.8f, 1f))); // Baju Biru
        Material pants = new Material(ColorAttribute.createDiffuse(new Color(0.1f, 0.2f, 0.4f, 1f))); // Celana Gelap

        // Skala 1 unit = 1 blok arena. Tinggi total karakter ~1.8 blok

        // Kaki Kiri
        mb.node().id = "leg_L";
        mb.node().translation.set(-0.15f, 0.375f, 0f);
        mb.part("leg_L", GL20.GL_TRIANGLES, attr, pants).box(0.25f, 0.75f, 0.25f);
        
        // Kaki Kanan
        mb.node().id = "leg_R";
        mb.node().translation.set(0.15f, 0.375f, 0f);
        mb.part("leg_R", GL20.GL_TRIANGLES, attr, pants).box(0.25f, 0.75f, 0.25f);
        
        // Badan (Baju)
        mb.node().id = "body";
        mb.node().translation.set(0f, 1.125f, 0f);
        mb.part("body", GL20.GL_TRIANGLES, attr, shirt).box(0.6f, 0.75f, 0.3f);
        
        // Lengan Kiri
        mb.node().id = "arm_L";
        mb.node().translation.set(-0.425f, 1.125f, 0f);
        mb.part("arm_L", GL20.GL_TRIANGLES, attr, skin).box(0.25f, 0.75f, 0.25f);
        
        // Lengan Kanan
        mb.node().id = "arm_R";
        mb.node().translation.set(0.425f, 1.125f, 0f);
        mb.part("arm_R", GL20.GL_TRIANGLES, attr, skin).box(0.25f, 0.75f, 0.25f);
        
        // Kepala
        mb.node().id = "head";
        mb.node().translation.set(0f, 1.75f, 0f);
        mb.part("head", GL20.GL_TRIANGLES, attr, skin).box(0.5f, 0.5f, 0.5f);
        
        return mb.end();
    }
}