package org.carft.block.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class CharacterModel {
    public static final int STANCE_STAND = 0;
    public static final int STANCE_CROUCH = 1;
    public static final int STANCE_PRONE = 2;

    // Membuat rig karakter cowok lengkap dengan sendi (pivot) di bahu, leher, dan pinggul
    public static Model createMaleModel() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        Material skinMat = new Material(ColorAttribute.createDiffuse(new Color(0.92f, 0.74f, 0.60f, 1f)));
        Material hairMat = new Material(ColorAttribute.createDiffuse(new Color(0.18f, 0.12f, 0.08f, 1f)));
        Material shirtMat = new Material(ColorAttribute.createDiffuse(new Color(0.18f, 0.52f, 0.82f, 1f)));
        Material pantsMat = new Material(ColorAttribute.createDiffuse(new Color(0.15f, 0.20f, 0.35f, 1f)));
        Material shoeMat = new Material(ColorAttribute.createDiffuse(new Color(0.15f, 0.15f, 0.15f, 1f)));

        // Node Root (Pivot di pinggul, tinggi 0.75m saat berdiri)
        Node root = mb.node();
        root.id = "root";
        root.translation.set(0f, 0.75f, 0f);

        // Node Badan (Torso)
        Node body = mb.node();
        body.id = "body";
        body.AttachTo(root);
        body.translation.set(0f, 0f, 0f);
        MeshPartBuilder bodyPart = mb.part("body_mesh", GL20.GL_TRIANGLES, attr, shirtMat);
        BoxShapeBuilder.build(bodyPart, 0f, 0.375f, 0f, 0.50f, 0.75f, 0.26f);

        // Node Kepala (Pivot di leher)
        Node head = mb.node();
        head.id = "head";
        head.AttachTo(body);
        head.translation.set(0f, 0.75f, 0f);
        MeshPartBuilder headPart = mb.part("head_mesh", GL20.GL_TRIANGLES, attr, skinMat);
        BoxShapeBuilder.build(headPart, 0f, 0.24f, 0f, 0.46f, 0.46f, 0.46f);
        MeshPartBuilder hairPart = mb.part("hair_mesh", GL20.GL_TRIANGLES, attr, hairMat);
        BoxShapeBuilder.build(hairPart, 0f, 0.43f, 0f, 0.48f, 0.14f, 0.48f);

        // Node Lengan Kiri (Pivot di bahu kiri)
        Node armL = mb.node();
        armL.id = "arm_L";
        armL.AttachTo(body);
        armL.translation.set(-0.37f, 0.70f, 0f);
        MeshPartBuilder sleeveL = mb.part("sleeve_L", GL20.GL_TRIANGLES, attr, shirtMat);
        BoxShapeBuilder.build(sleeveL, 0f, -0.10f, 0f, 0.24f, 0.28f, 0.24f);
        MeshPartBuilder armLPart = mb.part("arm_L_mesh", GL20.GL_TRIANGLES, attr, skinMat);
        BoxShapeBuilder.build(armLPart, 0f, -0.42f, 0f, 0.22f, 0.46f, 0.22f);

        // Node Lengan Kanan (Pivot di bahu kanan)
        Node armR = mb.node();
        armR.id = "arm_R";
        armR.AttachTo(body);
        armR.translation.set(0.37f, 0.70f, 0f);
        MeshPartBuilder sleeveR = mb.part("sleeve_R", GL20.GL_TRIANGLES, attr, shirtMat);
        BoxShapeBuilder.build(sleeveR, 0f, -0.10f, 0f, 0.24f, 0.28f, 0.24f);
        MeshPartBuilder armRPart = mb.part("arm_R_mesh", GL20.GL_TRIANGLES, attr, skinMat);
        BoxShapeBuilder.build(armRPart, 0f, -0.42f, 0f, 0.22f, 0.46f, 0.22f);

        // Node Kaki Kiri (Pivot di pangkal paha kiri)
        Node legL = mb.node();
        legL.id = "leg_L";
        legL.AttachTo(root);
        legL.translation.set(-0.13f, 0f, 0f);
        MeshPartBuilder legLPart = mb.part("leg_L_mesh", GL20.GL_TRIANGLES, attr, pantsMat);
        BoxShapeBuilder.build(legLPart, 0f, -0.32f, 0f, 0.24f, 0.64f, 0.24f);
        MeshPartBuilder shoeLPart = mb.part("shoe_L_mesh", GL20.GL_TRIANGLES, attr, shoeMat);
        BoxShapeBuilder.build(shoeLPart, 0f, -0.69f, 0.03f, 0.25f, 0.12f, 0.30f);

        // Node Kaki Kanan (Pivot di pangkal paha kanan)
        Node legR = mb.node();
        legR.id = "leg_R";
        legR.AttachTo(root);
        legR.translation.set(0.13f, 0f, 0f);
        MeshPartBuilder legRPart = mb.part("leg_R_mesh", GL20.GL_TRIANGLES, attr, pantsMat);
        BoxShapeBuilder.build(legRPart, 0f, -0.32f, 0f, 0.24f, 0.64f, 0.24f);
        MeshPartBuilder shoeRPart = mb.part("shoe_R_mesh", GL20.GL_TRIANGLES, attr, shoeMat);
        BoxShapeBuilder.build(shoeRPart, 0f, -0.69f, 0.03f, 0.25f, 0.12f, 0.30f);

        return mb.end();
    }

    // Class pengatur animasi karakter (Tanpa alokasi objek baru di render loop)
    public static class Animator {
        public final ModelInstance instance;
        private final Node root, body, head, armL, armR, legL, legR;
        private float animTimer = 0f;

        public Animator(Model model) {
            this.instance = new ModelInstance(model);
            this.root = instance.getNode("root");
            this.body = instance.getNode("body");
            this.head = instance.getNode("head");
            this.armL = instance.getNode("arm_L");
            this.armR = instance.getNode("arm_R");
            this.legL = instance.getNode("leg_L");
            this.legR = instance.getNode("leg_R");
        }

        public void update(float delta, Vector3 worldPos, float bodyYaw, float headPitch,
                           float moveSpeedRatio, boolean onGround, int stance, boolean isDolphinDiving) {

            // Reset rotasi dasar sendi
            root.rotation.idt();
            body.rotation.idt();
            head.rotation.idt();
            armL.rotation.idt();
            armR.rotation.idt();
            legL.rotation.idt();
            legR.rotation.idt();

            boolean isMoving = moveSpeedRatio > 0.08f;
            boolean isRunning = moveSpeedRatio > 0.72f && stance == STANCE_STAND;

            // Kecepatan siklus animasi kaki & tangan
            float freq = isRunning ? 12f : (stance == STANCE_CROUCH ? 6f : (stance == STANCE_PRONE ? 5f : 8f));
            if (isMoving && onGround) {
                animTimer += delta * freq;
            } else {
                animTimer += delta * 2.5f;
            }

            float swing = isMoving ? MathUtils.sin(animTimer) : 0f;
            float breath = MathUtils.sin(animTimer) * 3f;

            if (isDolphinDiving && !onGround) {
                // ANIMASI SPESIAL: Lari + Lompat + Klik Prone -> Melompat sambil TELENTANG di udara!
                root.translation.set(0f, 0.95f, 0f);
                // Badan rebah telentang menghadap langit (-78 derajat)
                root.rotation.setEulerAngles(0f, -78f, 0f);
                body.rotation.setEulerAngles(0f, -10f, 0f);
                head.rotation.setEulerAngles(0f, 25f, 0f);

                // Tangan dan kaki merentang dramatis di udara
                armL.rotation.setEulerAngles(0f, -125f, -35f);
                armR.rotation.setEulerAngles(0f, -125f, 35f);
                legL.rotation.setEulerAngles(0f, 25f, -18f);
                legR.rotation.setEulerAngles(0f, 25f, 18f);

            } else if (!onGround) {
                // ANIMASI LOMPAT BIASA DI UDARA
                root.translation.set(0f, 0.82f, 0f);
                body.rotation.setEulerAngles(0f, 12f, 0f);
                head.rotation.setEulerAngles(0f, MathUtils.clamp(-headPitch * 0.5f, -35f, 35f), 0f);

                armL.rotation.setEulerAngles(0f, -140f, -15f);
                armR.rotation.setEulerAngles(0f, -35f, 10f);
                legL.rotation.setEulerAngles(0f, 35f, 0f);
                legR.rotation.setEulerAngles(0f, -30f, 0f);

            } else if (stance == STANCE_PRONE) {
                // ANIMASI TIARAP (PRONE) & MERAYAP
                root.translation.set(0f, 0.18f, 0f);
                root.rotation.setEulerAngles(0f, 88f, 0f); // Badan rata dengan tanah
                head.rotation.setEulerAngles(0f, -68f, 0f); // Kepala mendongak ke depan

                float crawl = swing * 28f;
                armL.rotation.setEulerAngles(0f, -150f + crawl, -15f);
                armR.rotation.setEulerAngles(0f, -150f - crawl, 15f);
                legL.rotation.setEulerAngles(0f, -crawl * 0.7f, -8f);
                legR.rotation.setEulerAngles(0f, crawl * 0.7f, 8f);

            } else if (stance == STANCE_CROUCH) {
                // ANIMASI JONGKOK (CROUCH)
                root.translation.set(0f, 0.52f, 0f);
                body.rotation.setEulerAngles(0f, 28f, 0f); // Badan membungkuk ke depan
                head.rotation.setEulerAngles(0f, -20f + MathUtils.clamp(-headPitch * 0.4f, -30f, 30f), 0f);

                float crouchWalk = swing * 32f;
                // Kaki ditekuk jongkok
                legL.rotation.setEulerAngles(0f, -35f + crouchWalk, 0f);
                legR.rotation.setEulerAngles(0f, -35f - crouchWalk, 0f);
                // Tangan siaga memegang senjata
                armL.rotation.setEulerAngles(0f, -45f - crouchWalk * 0.5f, 0f);
                armR.rotation.setEulerAngles(0f, -65f + crouchWalk * 0.3f, 0f);

            } else {
                // ANIMASI BERDIRI / JALAN / LARI (STAND)
                float bounceY = isMoving ? Math.abs(MathUtils.cos(animTimer)) * (isRunning ? 0.08f : 0.04f) : 0f;
                root.translation.set(0f, 0.75f + bounceY, 0f);

                // Saat lari, badan condong ke depan 16 derajat
                float torsoLean = isRunning ? 16f : 0f;
                body.rotation.setEulerAngles(0f, torsoLean, 0f);
                head.rotation.setEulerAngles(0f, MathUtils.clamp(-headPitch * 0.5f, -40f, 40f) - torsoLean * 0.5f, 0f);

                float maxAngle = isRunning ? 62f : 38f;
                float limbAngle = swing * maxAngle;

                legL.rotation.setEulerAngles(0f, limbAngle, 0f);
                legR.rotation.setEulerAngles(0f, -limbAngle, 0f);

                if (isMoving) {
                    armL.rotation.setEulerAngles(0f, -limbAngle * 0.9f, -5f);
                    armR.rotation.setEulerAngles(0f, limbAngle * 0.9f, 5f);
                } else {
                    // Idle bernapas
                    armL.rotation.setEulerAngles(0f, breath, -4f);
                    armR.rotation.setEulerAngles(0f, -45f + breath, 4f); // Tangan kanan siap tembak
                }
            }

            // Set posisi & rotasi karakter di dunia 3D
            instance.transform.idt();
            instance.transform.setToTranslation(worldPos.x, worldPos.y, worldPos.z);
            instance.transform.rotate(Vector3.Y, bodyYaw);
            instance.calculateTransforms();
        }
    }
}