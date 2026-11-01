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
    public static final int STANCE_CLIMB = 3;

    public static Model createMaleModel() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        Material skinMat = new Material(ColorAttribute.createDiffuse(new Color(0.92f, 0.74f, 0.60f, 1f)));
        Material hairMat = new Material(ColorAttribute.createDiffuse(new Color(0.18f, 0.12f, 0.08f, 1f)));
        Material shirtMat = new Material(ColorAttribute.createDiffuse(new Color(0.18f, 0.52f, 0.82f, 1f)));
        Material pantsMat = new Material(ColorAttribute.createDiffuse(new Color(0.15f, 0.20f, 0.35f, 1f)));
        Material shoeMat = new Material(ColorAttribute.createDiffuse(new Color(0.15f, 0.15f, 0.15f, 1f)));

        Node root = mb.node();
        root.id = "root";

        Node body = mb.node();
        body.id = "body";
        body.translation.set(0f, 0f, 0f);
        MeshPartBuilder b = mb.part("body_mesh", GL20.GL_TRIANGLES, attr, shirtMat);
        BoxShapeBuilder.build(b, 0f, 0.375f, 0f, 0.50f, 0.75f, 0.26f);

        Node head = mb.node();
        head.id = "head";
        head.translation.set(0f, 0.75f, 0f);
        MeshPartBuilder h = mb.part("head_mesh", GL20.GL_TRIANGLES, attr, skinMat);
        BoxShapeBuilder.build(h, 0f, 0.24f, 0f, 0.46f, 0.46f, 0.46f);
        MeshPartBuilder hair = mb.part("hair_mesh", GL20.GL_TRIANGLES, attr, hairMat);
        BoxShapeBuilder.build(hair, 0f, 0.43f, 0f, 0.48f, 0.14f, 0.48f);

        Node armL = mb.node();
        armL.id = "arm_L";
        armL.translation.set(-0.37f, 0.70f, 0f);
        MeshPartBuilder slL = mb.part("sleeve_L", GL20.GL_TRIANGLES, attr, shirtMat);
        BoxShapeBuilder.build(slL, 0f, -0.10f, 0f, 0.24f, 0.28f, 0.24f);
        MeshPartBuilder aL = mb.part("arm_L_mesh", GL20.GL_TRIANGLES, attr, skinMat);
        BoxShapeBuilder.build(aL, 0f, -0.42f, 0f, 0.22f, 0.46f, 0.22f);

        Node armR = mb.node();
        armR.id = "arm_R";
        armR.translation.set(0.37f, 0.70f, 0f);
        MeshPartBuilder slR = mb.part("sleeve_R", GL20.GL_TRIANGLES, attr, shirtMat);
        BoxShapeBuilder.build(slR, 0f, -0.10f, 0f, 0.24f, 0.28f, 0.24f);
        MeshPartBuilder aR = mb.part("arm_R_mesh", GL20.GL_TRIANGLES, attr, skinMat);
        BoxShapeBuilder.build(aR, 0f, -0.42f, 0f, 0.22f, 0.46f, 0.22f);

        Node legL = mb.node();
        legL.id = "leg_L";
        legL.translation.set(-0.13f, 0f, 0f);
        MeshPartBuilder lL = mb.part("leg_L_mesh", GL20.GL_TRIANGLES, attr, pantsMat);
        BoxShapeBuilder.build(lL, 0f, -0.32f, 0f, 0.24f, 0.64f, 0.24f);
        MeshPartBuilder shL = mb.part("shoe_L_mesh", GL20.GL_TRIANGLES, attr, shoeMat);
        BoxShapeBuilder.build(shL, 0f, -0.69f, 0.03f, 0.25f, 0.12f, 0.30f);

        Node legR = mb.node();
        legR.id = "leg_R";
        legR.translation.set(0.13f, 0f, 0f);
        MeshPartBuilder lR = mb.part("leg_R_mesh", GL20.GL_TRIANGLES, attr, pantsMat);
        BoxShapeBuilder.build(lR, 0f, -0.32f, 0f, 0.24f, 0.64f, 0.24f);
        MeshPartBuilder shR = mb.part("shoe_R_mesh", GL20.GL_TRIANGLES, attr, shoeMat);
        BoxShapeBuilder.build(shR, 0f, -0.69f, 0.03f, 0.25f, 0.12f, 0.30f);

        Model model = mb.end();

        model.nodes.removeValue(body, true);
        model.nodes.removeValue(head, true);
        model.nodes.removeValue(armL, true);
        model.nodes.removeValue(armR, true);
        model.nodes.removeValue(legL, true);
        model.nodes.removeValue(legR, true);

        root.addChild(body);
        root.addChild(legL);
        root.addChild(legR);
        body.addChild(head);
        body.addChild(armL);
        body.addChild(armR);

        return model;
    }

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
                           float moveSpeedRatio, boolean onGround, int stance, boolean isDolphinDiving, 
                           boolean isSliding, boolean isClimbing, boolean isReloading, float reloadProgress) {

            root.rotation.idt();
            body.rotation.idt();
            head.rotation.idt();
            armL.rotation.idt();
            armR.rotation.idt();
            legL.rotation.idt();
            legR.rotation.idt();

            boolean isMoving = moveSpeedRatio > 0.08f;
            boolean isRunning = moveSpeedRatio > 0.72f && stance == STANCE_STAND;

            float freq = isRunning ? 12f : (stance == STANCE_CROUCH ? 6f : (stance == STANCE_PRONE ? 5f : 8f));
            if ((isMoving && onGround) || isClimbing) {
                animTimer += delta * freq;
            } else {
                animTimer += delta * 2.5f;
            }

            float swing = isMoving || isClimbing ? MathUtils.sin(animTimer) : 0f;
            float breath = MathUtils.sin(animTimer) * 2.5f;

            if (isClimbing) {
                root.translation.set(0f, 0.75f, 0.4f);
                body.rotation.setEulerAngles(0f, 15f, 0f);
                head.rotation.setEulerAngles(0f, MathUtils.clamp(-headPitch * 0.6f, -30f, 30f) - 20f, 0f);
                float climbCycle = swing * 40f;
                armL.rotation.setEulerAngles(0f, -150f + climbCycle, -10f);
                armR.rotation.setEulerAngles(0f, -150f - climbCycle, 10f);
                legL.rotation.setEulerAngles(0f, 35f - climbCycle, 0f);
                legR.rotation.setEulerAngles(0f, 35f + climbCycle, 0f);

            } else if (isDolphinDiving && !onGround) {
                root.translation.set(0f, 0.95f, 0f);
                root.rotation.setEulerAngles(0f, 75f, 0f);
                body.rotation.setEulerAngles(0f, 10f, 0f);
                head.rotation.setEulerAngles(0f, -25f, 0f);
                armL.rotation.setEulerAngles(0f, -145f, -35f);
                armR.rotation.setEulerAngles(0f, -145f, 35f);
                legL.rotation.setEulerAngles(0f, 20f, -10f);
                legR.rotation.setEulerAngles(0f, 20f, 10f);

            } else if (isSliding) {
                root.translation.set(0f, 0.35f, 0f); 
                root.rotation.setEulerAngles(0f, -15f, 0f); 
                body.rotation.setEulerAngles(0f, -15f, 0f); 
                head.rotation.setEulerAngles(0f, 30f + MathUtils.clamp(-headPitch * 0.6f, -35f, 35f), 0f); 
                
                legL.rotation.setEulerAngles(0f, -80f, 0f);
                legR.rotation.setEulerAngles(0f, 40f, 0f);
                armR.rotation.setEulerAngles(0f, -80f, 10f);
                armL.rotation.setEulerAngles(0f, -65f, -35f);

            } else if (!onGround) {
                root.translation.set(0f, 0.82f, 0f);
                body.rotation.setEulerAngles(0f, 12f, 0f);
                head.rotation.setEulerAngles(0f, MathUtils.clamp(-headPitch * 0.5f, -35f, 35f), 0f);
                armL.rotation.setEulerAngles(0f, -70f, -30f);
                armR.rotation.setEulerAngles(0f, -75f, 10f);
                legL.rotation.setEulerAngles(0f, 35f, 0f);
                legR.rotation.setEulerAngles(0f, -30f, 0f);

            } else if (stance == STANCE_PRONE) {
                root.translation.set(0f, 0.18f, 0f);
                root.rotation.setEulerAngles(0f, 85f, 0f); 
                head.rotation.setEulerAngles(0f, -68f, 0f);
                float crawl = swing * 28f;
                armL.rotation.setEulerAngles(0f, -150f + crawl, -15f);
                armR.rotation.setEulerAngles(0f, -150f - crawl, 15f);
                legL.rotation.setEulerAngles(0f, -crawl * 0.7f, -8f);
                legR.rotation.setEulerAngles(0f, crawl * 0.7f, 8f);

            } else if (stance == STANCE_CROUCH) {
                root.translation.set(0f, 0.52f, 0f);
                body.rotation.setEulerAngles(0f, 25f, 0f);
                head.rotation.setEulerAngles(0f, -18f + MathUtils.clamp(-headPitch * 0.4f, -30f, 30f), 0f);
                float crouchWalk = swing * 30f;
                legL.rotation.setEulerAngles(0f, -35f + crouchWalk, 0f);
                legR.rotation.setEulerAngles(0f, -35f - crouchWalk, 0f);
                
                armR.rotation.setEulerAngles(0f, -75f, 10f);
                if (isReloading) {
                    float reloadSw = MathUtils.sin(reloadProgress * MathUtils.PI) * 45f;
                    armL.rotation.setEulerAngles(0f, -25f - reloadSw, -15f);
                } else {
                    armL.rotation.setEulerAngles(0f, -65f, -35f);
                }

            } else {
                float bounceY = isMoving ? Math.abs(MathUtils.cos(animTimer)) * (isRunning ? 0.08f : 0.04f) : 0f;
                root.translation.set(0f, 0.75f + bounceY, 0f);
                float torsoLean = isRunning ? 16f : 0f;
                body.rotation.setEulerAngles(0f, torsoLean, 0f);
                head.rotation.setEulerAngles(0f, MathUtils.clamp(-headPitch * 0.5f, -40f, 40f) - torsoLean * 0.5f, 0f);
                float maxAngle = isRunning ? 55f : 35f;
                float limbAngle = swing * maxAngle;
                legL.rotation.setEulerAngles(0f, limbAngle, 0f);
                legR.rotation.setEulerAngles(0f, -limbAngle, 0f);
                
                // TPP Gun Grip 2 Tangan & Tactical Sprint
                if (isRunning) {
                    // Senjata diangkat miring saat sprint
                    armR.rotation.setEulerAngles(0f, -95f, 15f);
                    armL.rotation.setEulerAngles(0f, -75f, -40f);
                } else {
                    armR.rotation.setEulerAngles(0f, -78f + breath, 8f);
                    if (isReloading) {
                        float reloadSw = MathUtils.sin(reloadProgress * MathUtils.PI) * 50f;
                        armL.rotation.setEulerAngles(0f, -30f - reloadSw, -15f);
                    } else {
                        // Tangan kiri menopang laras
                        armL.rotation.setEulerAngles(0f, -65f + breath, -38f);
                    }
                }
            }

            instance.transform.idt();
            instance.transform.setToTranslation(worldPos.x, worldPos.y, worldPos.z);
            instance.transform.rotate(Vector3.Y, bodyYaw);
            instance.calculateTransforms();
        }
    }
}