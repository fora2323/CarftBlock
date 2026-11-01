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
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class GB18 implements Disposable {

    public enum WeaponState { HOLSTERED, EQUIPPING, IDLE, FIRING, RELOADING, UNEQUIPPING }

    private static final int MAG_SIZE = 12;
    private static final float FIRE_RATE = 0.22f;
    private static final float EQUIP_TIME = 0.38f;
    private static final float UNEQUIP_TIME = 0.32f;
    private static final float RELOAD_TIME = 1.8f;
    private static final float RECOIL_KICK = 9f;
    private static final float RECOIL_RECOVER = 14f;

    public int ammoInMag = MAG_SIZE;
    public int ammoReserve = 48;
    public WeaponState state = WeaponState.HOLSTERED;

    private final Model gb18Model;
    private final Model bulletModel;
    private final Model casingModel;
    private final Model muzzleFlashModel;

    public final ModelInstance gb18Instance;
    private ModelInstance muzzleFlashInstance;
    private boolean showMuzzleFlash = false;
    private float muzzleFlashTimer = 0f;

    public final Array<AK47.Bullet> bullets = new Array<>();
    public final Array<AK47.Casing> casings = new Array<>();

    private float stateTimer = 0f;
    private float fireTimer = 0f;
    private float recoilPitch = 0f;

    private final Vector3 holsterPos = new Vector3(-0.16f, 0.88f, 0.05f);
    private final Vector3 equipPos = new Vector3(0.22f, 1.38f, -0.32f);
    private final Vector3 currentPos = new Vector3();
    private final Quaternion currentRot = new Quaternion();
    private final Quaternion holsterRot = new Quaternion().setEulerAngles(0f, -25f, 90f);
    private final Quaternion equipRot = new Quaternion().setEulerAngles(0f, 0f, 0f);

    public GB18() {
        gb18Model = buildGb18Model();
        bulletModel = buildBulletModel();
        casingModel = buildCasingModel();
        muzzleFlashModel = buildMuzzleFlashModel();

        gb18Instance = new ModelInstance(gb18Model);
        muzzleFlashInstance = new ModelInstance(muzzleFlashModel);

        currentPos.set(holsterPos);
        currentRot.set(holsterRot);
    }

    private Model buildGb18Model() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        Material black = new Material(ColorAttribute.createDiffuse(new Color(0.10f, 0.10f, 0.10f, 1f)));
        Material darkGray = new Material(ColorAttribute.createDiffuse(new Color(0.18f, 0.18f, 0.18f, 1f)));
        Material midGray = new Material(ColorAttribute.createDiffuse(new Color(0.28f, 0.28f, 0.28f, 1f)));
        Material silver = new Material(ColorAttribute.createDiffuse(new Color(0.55f, 0.55f, 0.58f, 1f)));

        Node root = mb.node(); root.id = "gb18_root";

        // Slide (bagian atas yang bergerak saat tembak)
        BoxShapeBuilder.build(mb.part("slide", GL20.GL_TRIANGLES, attr, darkGray),
                0f, 0.014f, -0.045f, 0.048f, 0.052f, 0.155f);

        // Frame / badan bawah
        BoxShapeBuilder.build(mb.part("frame", GL20.GL_TRIANGLES, attr, black),
                0f, -0.010f, -0.025f, 0.044f, 0.042f, 0.118f);

        // Barrel ujung
        BoxShapeBuilder.build(mb.part("barrel", GL20.GL_TRIANGLES, attr, silver),
                0f, 0.014f, -0.128f, 0.018f, 0.018f, 0.028f);

        // Grip
        BoxShapeBuilder.build(mb.part("grip", GL20.GL_TRIANGLES, attr, black),
                0f, -0.060f, 0.028f, 0.040f, 0.082f, 0.038f);

        // Trigger guard
        BoxShapeBuilder.build(mb.part("triggerguard", GL20.GL_TRIANGLES, attr, black),
                0f, -0.030f, -0.010f, 0.010f, 0.018f, 0.038f);

        // Magazine base
        BoxShapeBuilder.build(mb.part("magbase", GL20.GL_TRIANGLES, attr, darkGray),
                0f, -0.102f, 0.028f, 0.038f, 0.014f, 0.036f);

        // Front sight
        BoxShapeBuilder.build(mb.part("frontsight", GL20.GL_TRIANGLES, attr, black),
                0f, 0.044f, -0.120f, 0.008f, 0.014f, 0.008f);

        // Rear sight
        BoxShapeBuilder.build(mb.part("rearsight", GL20.GL_TRIANGLES, attr, black),
                0f, 0.044f, -0.002f, 0.020f, 0.012f, 0.010f);

        // Rail bawah
        BoxShapeBuilder.build(mb.part("rail", GL20.GL_TRIANGLES, attr, midGray),
                0f, -0.008f, -0.075f, 0.036f, 0.008f, 0.068f);

        Model model = mb.end();
        return model;
    }

    private Model buildBulletModel() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        Material brass = new Material(ColorAttribute.createDiffuse(new Color(0.85f, 0.72f, 0.18f, 1f)));
        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Node r = mb.node(); r.id = "bullet";
        BoxShapeBuilder.build(mb.part("b", GL20.GL_TRIANGLES, attr, brass),
                0f, 0f, 0f, 0.008f, 0.008f, 0.028f);
        return mb.end();
    }

    private Model buildCasingModel() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        Material brass = new Material(ColorAttribute.createDiffuse(new Color(0.78f, 0.62f, 0.14f, 1f)));
        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Node r = mb.node(); r.id = "casing";
        BoxShapeBuilder.build(mb.part("c", GL20.GL_TRIANGLES, attr, brass),
                0f, 0f, 0f, 0.007f, 0.007f, 0.018f);
        return mb.end();
    }

    private Model buildMuzzleFlashModel() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        Material flash = new Material(ColorAttribute.createDiffuse(new Color(1f, 0.88f, 0.25f, 1f)));
        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Node r = mb.node(); r.id = "flash";
        BoxShapeBuilder.build(mb.part("f", GL20.GL_TRIANGLES, attr, flash),
                0f, 0f, 0f, 0.032f, 0.032f, 0.038f);
        return mb.end();
    }

    public void equip() {
        if (state == WeaponState.HOLSTERED) { state = WeaponState.EQUIPPING; stateTimer = 0f; }
    }

    public void unequip() {
        if (state == WeaponState.IDLE || state == WeaponState.FIRING) { state = WeaponState.UNEQUIPPING; stateTimer = 0f; }
    }

    public void triggerFire(Vector3 muzzleWorldPos, Vector3 fireDir, Vector3 rightDir) {
        if (state != WeaponState.IDLE && state != WeaponState.FIRING) return;
        if (ammoInMag <= 0 || fireTimer > 0f) return;

        ammoInMag--;
        fireTimer = FIRE_RATE;
        recoilPitch += RECOIL_KICK;
        state = WeaponState.FIRING;

        ModelInstance bi = new ModelInstance(bulletModel);
        bi.transform.setToTranslation(muzzleWorldPos);
        bullets.add(new AK47.Bullet(bi, fireDir));

        Vector3 ejPos = new Vector3(muzzleWorldPos).add(rightDir.x * 0.04f, 0.015f, rightDir.z * 0.04f);
        ModelInstance ci = new ModelInstance(casingModel);
        ci.transform.setToTranslation(ejPos);
        casings.add(new AK47.Casing(ci, rightDir));

        showMuzzleFlash = true;
        muzzleFlashTimer = 0.042f;
        muzzleFlashInstance.transform.setToTranslation(
                muzzleWorldPos.x + fireDir.x * 0.03f,
                muzzleWorldPos.y + fireDir.y * 0.03f,
                muzzleWorldPos.z + fireDir.z * 0.03f);
    }

    public void triggerReload() {
        if (state == WeaponState.IDLE && ammoInMag < MAG_SIZE && ammoReserve > 0) {
            state = WeaponState.RELOADING; stateTimer = 0f;
        }
    }

    public void update(float delta, Vector3 playerPos, float yaw, boolean thirdPerson,
                       Node armRNode, Matrix4 charTransform) {
        if (fireTimer > 0f) fireTimer -= delta;
        recoilPitch = MathUtils.lerp(recoilPitch, 0f, Math.min(1f, delta * RECOIL_RECOVER));
        if (recoilPitch < 0.1f && state == WeaponState.FIRING) state = WeaponState.IDLE;

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

            case RELOADING:
                float rp = stateTimer / RELOAD_TIME;
                float dropY = rp < 0.25f ? MathUtils.sin(rp / 0.25f * MathUtils.PI) * -0.065f : 0f;
                currentPos.set(equipPos).add(0f, dropY, 0f);
                if (rp > 0.4f && rp < 0.7f) {
                    float ins = (rp - 0.4f) / 0.3f;
                    currentPos.add(0f, MathUtils.sin(ins * MathUtils.PI) * 0.05f, 0f);
                }
                if (stateTimer >= RELOAD_TIME) {
                    int needed = MAG_SIZE - ammoInMag;
                    int take = Math.min(needed, ammoReserve);
                    ammoInMag += take;
                    ammoReserve -= take;
                    state = WeaponState.IDLE;
                    currentPos.set(equipPos);
                }
                break;

            case IDLE:
            case FIRING:
                float breathX = MathUtils.sin(stateTimer * 1.8f) * 0.002f;
                float breathY = MathUtils.sin(stateTimer * 1.1f) * 0.002f;
                currentPos.set(equipPos).add(breathX, breathY - recoilPitch * 0.003f, recoilPitch * 0.002f);
                break;

            default:
                currentPos.set(holsterPos);
                currentRot.set(holsterRot);
                break;
        }

        if (thirdPerson && armRNode != null) {
            Matrix4 armWorld = armRNode.globalTransform.cpy().mulLeft(charTransform);
            gb18Instance.transform.set(armWorld);
            gb18Instance.transform.translate(currentPos);
            gb18Instance.transform.rotate(currentRot);
        } else {
            float rad = (float) Math.toRadians(yaw);
            Vector3 wp = new Vector3(
                    playerPos.x + (float)Math.sin(rad) * currentPos.z - (float)Math.cos(rad) * currentPos.x,
                    playerPos.y + currentPos.y,
                    playerPos.z + (float)Math.cos(rad) * currentPos.z + (float)Math.sin(rad) * currentPos.x
            );
            gb18Instance.transform.idt();
            gb18Instance.transform.setToTranslation(wp);
            gb18Instance.transform.rotate(Vector3.Y, yaw);
            gb18Instance.transform.rotate(currentRot);
        }

        if (showMuzzleFlash) {
            muzzleFlashTimer -= delta;
            if (muzzleFlashTimer <= 0f) showMuzzleFlash = false;
        }

        for (int i = bullets.size - 1; i >= 0; i--) {
            AK47.Bullet bullet = bullets.get(i);
            bullet.life += delta;
            if (bullet.life >= AK47.Bullet.MAX_LIFE) { bullets.removeIndex(i); continue; }
            bullet.instance.transform.translate(bullet.velocity.x * delta, bullet.velocity.y * delta, bullet.velocity.z * delta);
        }

        for (int i = casings.size - 1; i >= 0; i--) {
            AK47.Casing casing = casings.get(i);
            casing.life += delta;
            if (casing.life >= AK47.Casing.MAX_LIFE) { casings.removeIndex(i); continue; }
            casing.velocity.y -= 9.8f * delta;
            casing.instance.transform.translate(casing.velocity.x * delta, casing.velocity.y * delta, casing.velocity.z * delta);
            casing.instance.transform.rotate(Vector3.X, casing.rotX * delta);
            casing.instance.transform.rotate(Vector3.Z, casing.rotZ * delta);
        }
    }

    public void render(ModelBatch batch, Environment env) {
        if (state != WeaponState.HOLSTERED) batch.render(gb18Instance, env);
        if (showMuzzleFlash) batch.render(muzzleFlashInstance, env);
        for (AK47.Bullet b : bullets) batch.render(b.instance, env);
        for (AK47.Casing c : casings) batch.render(c.instance, env);
    }

    public boolean isHolstered() { return state == WeaponState.HOLSTERED; }
    public boolean isReloading() { return state == WeaponState.RELOADING; }
    public boolean canFire() { return (state == WeaponState.IDLE || state == WeaponState.FIRING) && fireTimer <= 0f && ammoInMag > 0; }
    public float getRecoilPitch() { return recoilPitch; }
    public int getAmmoInMag() { return ammoInMag; }
    public int getAmmoReserve() { return ammoReserve; }

    @Override
    public void dispose() {
        gb18Model.dispose();
        bulletModel.dispose();
        casingModel.dispose();
        muzzleFlashModel.dispose();
    }
}