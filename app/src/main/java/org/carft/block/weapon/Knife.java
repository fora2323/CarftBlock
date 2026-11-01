package org.carft.block.weapon;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

public class Knife implements Disposable {

    public enum WeaponState { HOLSTERED, EQUIPPING, IDLE, ATTACKING, UNEQUIPPING }
    public enum AttackType { SLASH_H, SLASH_V, STAB, BACKHAND }

    private static final float EQUIP_TIME = 0.30f;
    private static final float UNEQUIP_TIME = 0.25f;
    private static final float ATTACK_TIME = 0.38f;
    private static final float ATTACK_COOLDOWN = 0.22f;

    public WeaponState state = WeaponState.HOLSTERED;
    private AttackType currentAttack = AttackType.SLASH_H;

    private final Model knifeModel;
    public final ModelInstance knifeInstance;

    private float stateTimer = 0f;
    private float attackCooldown = 0f;
    private int attackCount = 0;

    private final Vector3 holsterPos = new Vector3(-0.14f, 0.72f, 0.06f);
    private final Vector3 equipPos = new Vector3(0.24f, 1.35f, -0.28f);
    private final Vector3 currentPos = new Vector3();
    private final Quaternion currentRot = new Quaternion();
    private final Quaternion holsterRot = new Quaternion().setEulerAngles(0f, 45f, -80f);
    private final Quaternion equipRot = new Quaternion().setEulerAngles(0f, 0f, 0f);
    private final Quaternion attackRot = new Quaternion();

    public Knife() {
        knifeModel = buildKnifeModel();
        knifeInstance = new ModelInstance(knifeModel);
        currentPos.set(holsterPos);
        currentRot.set(holsterRot);
    }

    private Model buildKnifeModel() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        Material blade = new Material(ColorAttribute.createDiffuse(new Color(0.72f, 0.72f, 0.75f, 1f)));
        Material bladeEdge = new Material(ColorAttribute.createDiffuse(new Color(0.88f, 0.88f, 0.92f, 1f)));
        Material guard = new Material(ColorAttribute.createDiffuse(new Color(0.30f, 0.30f, 0.30f, 1f)));
        Material handle = new Material(ColorAttribute.createDiffuse(new Color(0.14f, 0.10f, 0.08f, 1f)));
        Material wrap = new Material(ColorAttribute.createDiffuse(new Color(0.22f, 0.16f, 0.10f, 1f)));

        Node root = mb.node(); root.id = "knife_root";

        // Blade utama
        BoxShapeBuilder.build(mb.part("blade_main", GL20.GL_TRIANGLES, attr, blade),
                0f, 0f, -0.075f, 0.022f, 0.008f, 0.150f);

        // Blade edge (lebih tipis, sedikit offset bawah)
        BoxShapeBuilder.build(mb.part("blade_edge", GL20.GL_TRIANGLES, attr, bladeEdge),
                0f, -0.003f, -0.068f, 0.018f, 0.003f, 0.136f);

        // Tip blade
        BoxShapeBuilder.build(mb.part("blade_tip", GL20.GL_TRIANGLES, attr, bladeEdge),
                0f, -0.001f, -0.150f, 0.012f, 0.005f, 0.022f);

        // Guard
        BoxShapeBuilder.build(mb.part("guard", GL20.GL_TRIANGLES, attr, guard),
                0f, 0f, 0.002f, 0.042f, 0.018f, 0.014f);

        // Handle badan
        BoxShapeBuilder.build(mb.part("handle", GL20.GL_TRIANGLES, attr, handle),
                0f, 0f, 0.058f, 0.026f, 0.022f, 0.095f);

        // Wrap tekstur handle (3 strip)
        BoxShapeBuilder.build(mb.part("wrap1", GL20.GL_TRIANGLES, attr, wrap),
                0f, 0f, 0.028f, 0.028f, 0.024f, 0.010f);
        BoxShapeBuilder.build(mb.part("wrap2", GL20.GL_TRIANGLES, attr, wrap),
                0f, 0f, 0.058f, 0.028f, 0.024f, 0.010f);
        BoxShapeBuilder.build(mb.part("wrap3", GL20.GL_TRIANGLES, attr, wrap),
                0f, 0f, 0.088f, 0.028f, 0.024f, 0.010f);

        // Pommel
        BoxShapeBuilder.build(mb.part("pommel", GL20.GL_TRIANGLES, attr, guard),
                0f, 0f, 0.115f, 0.030f, 0.026f, 0.018f);

        return mb.end();
    }

    public void equip() {
        if (state == WeaponState.HOLSTERED) { state = WeaponState.EQUIPPING; stateTimer = 0f; }
    }

    public void unequip() {
        if (state == WeaponState.IDLE) { state = WeaponState.UNEQUIPPING; stateTimer = 0f; }
    }

    public void triggerAttack() {
        if (state != WeaponState.IDLE || attackCooldown > 0f) return;
        state = WeaponState.ATTACKING;
        stateTimer = 0f;

        AttackType[] types = AttackType.values();
        if (MathUtils.randomBoolean(0.25f)) {
            currentAttack = types[MathUtils.random(types.length - 1)];
        } else {
            attackCount++;
            currentAttack = types[attackCount % 2];
        }
    }

    public boolean isHitFrame() {
        return state == WeaponState.ATTACKING && stateTimer >= ATTACK_TIME * 0.35f && stateTimer <= ATTACK_TIME * 0.65f;
    }

    public void update(float delta, Vector3 playerPos, float yaw, boolean thirdPerson,
                       Node armRNode, Matrix4 charTransform) {
        if (attackCooldown > 0f) attackCooldown -= delta;
        stateTimer += delta;

        switch (state) {
            case EQUIPPING:
                float ep = Math.min(1f, stateTimer / EQUIP_TIME);
                currentPos.set(holsterPos).lerp(equipPos, ep);
                currentRot.set(holsterRot).slerp(equipRot, ep);
                if (stateTimer >= EQUIP_TIME) state = WeaponState.IDLE;
                break;

            case UNEQUIPPING:
                float up = Math.min(1f, stateTimer / UNEQUIP_TIME);
                currentPos.set(equipPos).lerp(holsterPos, up);
                currentRot.set(equipRot).slerp(holsterRot, up);
                if (stateTimer >= UNEQUIP_TIME) state = WeaponState.HOLSTERED;
                break;

            case ATTACKING:
                float ap = stateTimer / ATTACK_TIME;
                float swing = MathUtils.sin(ap * MathUtils.PI);

                switch (currentAttack) {
                    case SLASH_H:
                        currentPos.set(equipPos).add(swing * 0.12f, -swing * 0.04f, swing * 0.05f);
                        attackRot.setEulerAngles(swing * -55f, 0f, swing * 25f);
                        break;
                    case SLASH_V:
                        currentPos.set(equipPos).add(0f, -swing * 0.10f, swing * 0.06f);
                        attackRot.setEulerAngles(swing * -75f, 0f, 0f);
                        break;
                    case STAB:
                        currentPos.set(equipPos).add(0f, 0f, -swing * 0.14f);
                        attackRot.setEulerAngles(swing * -15f, 0f, 0f);
                        break;
                    case BACKHAND:
                        currentPos.set(equipPos).add(-swing * 0.10f, swing * 0.03f, swing * 0.04f);
                        attackRot.setEulerAngles(swing * 45f, swing * 30f, swing * -20f);
                        break;
                }
                currentRot.set(equipRot).mul(attackRot);

                if (stateTimer >= ATTACK_TIME) {
                    state = WeaponState.IDLE;
                    attackCooldown = ATTACK_COOLDOWN;
                    currentPos.set(equipPos);
                    currentRot.set(equipRot);
                }
                break;

            case IDLE:
                float breathX = MathUtils.sin(stateTimer * 1.6f) * 0.002f;
                float breathY = MathUtils.sin(stateTimer * 1.0f) * 0.002f;
                currentPos.set(equipPos).add(breathX, breathY, 0f);
                currentRot.set(equipRot);
                break;

            default:
                currentPos.set(holsterPos);
                currentRot.set(holsterRot);
                break;
        }

        if (thirdPerson && armRNode != null) {
            Matrix4 armWorld = armRNode.globalTransform.cpy().mulLeft(charTransform);
            knifeInstance.transform.set(armWorld);
            knifeInstance.transform.translate(currentPos);
            knifeInstance.transform.rotate(currentRot);
        } else {
            float rad = (float) Math.toRadians(yaw);
            Vector3 wp = new Vector3(
                    playerPos.x + (float)Math.sin(rad) * currentPos.z - (float)Math.cos(rad) * currentPos.x,
                    playerPos.y + currentPos.y,
                    playerPos.z + (float)Math.cos(rad) * currentPos.z + (float)Math.sin(rad) * currentPos.x
            );
            knifeInstance.transform.idt();
            knifeInstance.transform.setToTranslation(wp);
            knifeInstance.transform.rotate(Vector3.Y, yaw);
            knifeInstance.transform.rotate(currentRot);
        }
    }

    public void render(ModelBatch batch, Environment env) {
        if (state != WeaponState.HOLSTERED) batch.render(knifeInstance, env);
    }

    public boolean isHolstered() { return state == WeaponState.HOLSTERED; }
    public boolean canAttack() { return state == WeaponState.IDLE && attackCooldown <= 0f; }

    @Override
    public void dispose() {
        knifeModel.dispose();
    }
}