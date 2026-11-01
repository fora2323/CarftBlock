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

public class AK47 implements Disposable {

    public enum WeaponState { HOLSTERED, EQUIPPING, IDLE, FIRING, RELOADING, UNEQUIPPING }

    private static final int MAG_SIZE = 30;
    private static final float FIRE_RATE = 0.1f;
    private static final float EQUIP_TIME = 0.5f;
    private static final float UNEQUIP_TIME = 0.4f;
    private static final float RELOAD_TIME = 2.2f;
    private static final float RECOIL_KICK = 6f;
    private static final float RECOIL_RECOVER = 12f;

    public int ammoInMag = MAG_SIZE;
    public int ammoReserve = 90;
    public WeaponState state = WeaponState.HOLSTERED;

    private final Model akModel;
    private final Model bulletModel;
    private final Model casingModel;
    private final Model muzzleFlashModel;

    public final ModelInstance akInstance;
    private ModelInstance muzzleFlashInstance;
    private boolean showMuzzleFlash = false;
    private float muzzleFlashTimer = 0f;

    public static class Bullet {
        public final ModelInstance instance;
        public final Vector3 velocity = new Vector3();
        public float life = 0f;
        public static final float MAX_LIFE = 0.18f;
        public Bullet(ModelInstance inst, Vector3 dir) {
            this.instance = inst;
            this.velocity.set(dir).scl(85f);
        }
    }

    public static class Casing {
        public final ModelInstance instance;
        public final Vector3 velocity = new Vector3();
        public float life = 0f;
        public float rotX = 0f, rotZ = 0f;
        public static final float MAX_LIFE = 1.8f;
        public Casing(ModelInstance inst, Vector3 ejectDir) {
            this.instance = inst;
            this.velocity.set(ejectDir).scl(3.5f);
            this.velocity.y += 2.2f;
            this.rotX = MathUtils.random(200f, 400f);
            this.rotZ = MathUtils.random(100f, 300f);
        }
    }

    public final Array<Bullet> bullets = new Array<>();
    public final Array<Casing> casings = new Array<>();

    private float stateTimer = 0f;
    private float fireTimer = 0f;
    private float recoilPitch = 0f;

    private final Vector3 holsterPos = new Vector3(0.18f, 0.92f, 0.05f);
    private final Vector3 equipPos = new Vector3(0.28f, 1.42f, -0.38f);
    private final Vector3 currentPos = new Vector3();
    private final Quaternion currentRot = new Quaternion();
    private final Quaternion holsterRot = new Quaternion().setEulerAngles(-90f, 15f, 0f);
    private final Quaternion equipRot = new Quaternion().setEulerAngles(0f, 0f, 0f);

    public AK47() {
        akModel = buildAkModel();
        bulletModel = buildBulletModel();
        casingModel = buildCasingModel();
        muzzleFlashModel = buildMuzzleFlashModel();

        akInstance = new ModelInstance(akModel);
        muzzleFlashInstance = new ModelInstance(muzzleFlashModel);

        currentPos.set(holsterPos);
        currentRot.set(holsterRot);
        applyTransform();
    }

    private Model buildAkModel() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        Material metalDark = new Material(ColorAttribute.createDiffuse(new Color(0.13f, 0.13f, 0.13f, 1f)));
        Material metalMid = new Material(ColorAttribute.createDiffuse(new Color(0.22f, 0.22f, 0.22f, 1f)));
        Material wood = new Material(ColorAttribute.createDiffuse(new Color(0.42f, 0.24f, 0.08f, 1f)));
        Material woodDark = new Material(ColorAttribute.createDiffuse(new Color(0.30f, 0.16f, 0.05f, 1f)));
        Material magazine = new Material(ColorAttribute.createDiffuse(new Color(0.18f, 0.18f, 0.18f, 1f)));

        Node root = mb.node(); root.id = "ak_root";

        // Receiver / badan utama
        mb.part("receiver", GL20.GL_TRIANGLES, attr, metalDark);
        BoxShapeBuilder.build(mb.part("receiver", GL20.GL_TRIANGLES, attr, metalDark),
                0f, 0f, 0f, 0.055f, 0.068f, 0.42f);

        // Barrel
        mb.part("barrel", GL20.GL_TRIANGLES, attr, metalMid);
        BoxShapeBuilder.build(mb.part("barrel", GL20.GL_TRIANGLES, attr, metalMid),
                0f, 0.008f, -0.28f, 0.028f, 0.028f, 0.26f);

        // Gas tube di atas barrel
        mb.part("gastube", GL20.GL_TRIANGLES, attr, metalDark);
        BoxShapeBuilder.build(mb.part("gastube", GL20.GL_TRIANGLES, attr, metalDark),
                0f, 0.042f, -0.18f, 0.018f, 0.014f, 0.18f);

        // Handguard kayu bawah barrel
        mb.part("handguard", GL20.GL_TRIANGLES, attr, wood);
        BoxShapeBuilder.build(mb.part("handguard", GL20.GL_TRIANGLES, attr, wood),
                0f, -0.008f, -0.16f, 0.038f, 0.032f, 0.20f);

        // Stock kayu
        mb.part("stock", GL20.GL_TRIANGLES, attr, wood);
        BoxShapeBuilder.build(mb.part("stock", GL20.GL_TRIANGLES, attr, wood),
                0f, -0.012f, 0.22f, 0.048f, 0.055f, 0.18f);

        // Stock butt plate
        mb.part("buttplate", GL20.GL_TRIANGLES, attr, woodDark);
        BoxShapeBuilder.build(mb.part("buttplate", GL20.GL_TRIANGLES, attr, woodDark),
                0f, -0.014f, 0.315f, 0.050f, 0.062f, 0.018f);

        // Pistol grip
        mb.part("grip", GL20.GL_TRIANGLES, attr, woodDark);
        BoxShapeBuilder.build(mb.part("grip", GL20.GL_TRIANGLES, attr, woodDark),
                0f, -0.072f, 0.085f, 0.034f, 0.095f, 0.040f);

        // Magazine
        mb.part("mag", GL20.GL_TRIANGLES, attr, magazine);
        BoxShapeBuilder.build(mb.part("mag", GL20.GL_TRIANGLES, attr, magazine),
                0f, -0.085f, -0.005f, 0.038f, 0.095f, 0.068f);

        // Trigger guard
        mb.part("triggerguard", GL20.GL_TRIANGLES, attr, metalDark);
        BoxShapeBuilder.build(mb.part("triggerguard", GL20.GL_TRIANGLES, attr, metalDark),
                0f, -0.038f, 0.042f, 0.010f, 0.022f, 0.045f);

        // Front sight
        mb.part("frontsight", GL20.GL_TRIANGLES, attr, metalDark);
        BoxShapeBuilder.build(mb.part("frontsight", GL20.GL_TRIANGLES, attr, metalDark),
                0f, 0.038f, -0.385f, 0.012f, 0.028f, 0.012f);

        // Rear sight
        mb.part("rearsight", GL20.GL_TRIANGLES, attr, metalDark);
        BoxShapeBuilder.build(mb.part("rearsight", GL20.GL_TRIANGLES, attr, metalDark),
                0f, 0.038f, 0.02f, 0.022f, 0.018f, 0.018f);

        // Charging handle
        mb.part("chargehandle", GL20.GL_TRIANGLES, attr, metalMid);
        BoxShapeBuilder.build(mb.part("chargehandle", GL20.GL_TRIANGLES, attr, metalMid),
                0.038f, 0.010f, 0.055f, 0.022f, 0.014f, 0.022f);

        Model model = mb.end();

        model.nodes.removeValue(root, true);
        Node fakeRoot = new Node(); fakeRoot.id = "ak_root";
        fakeRoot.addChild(root);
        model.nodes.add(fakeRoot);

        return model;
    }

    private Model buildBulletModel() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        Material brass = new Material(ColorAttribute.createDiffuse(new Color(0.85f, 0.72f, 0.18f, 1f)));
        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Node r = mb.node(); r.id = "bullet";
        BoxShapeBuilder.build(mb.part("b", GL20.GL_TRIANGLES, attr, brass),
                0f, 0f, 0f, 0.010f, 0.010f, 0.038f);
        return mb.end();
    }

    private Model buildCasingModel() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        Material brass = new Material(ColorAttribute.createDiffuse(new Color(0.78f, 0.62f, 0.14f, 1f)));
        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Node r = mb.node(); r.id = "casing";
        BoxShapeBuilder.build(mb.part("c", GL20.GL_TRIANGLES, attr, brass),
                0f, 0f, 0f, 0.009f, 0.009f, 0.025f);
        return mb.end();
    }

    private Model buildMuzzleFlashModel() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        Material flash = new Material(ColorAttribute.createDiffuse(new Color(1f, 0.92f, 0.3f, 1f)));
        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Node r = mb.node(); r.id = "flash";
        BoxShapeBuilder.build(mb.part("f", GL20.GL_TRIANGLES, attr, flash),
                0f, 0f, 0f, 0.045f, 0.045f, 0.055f);
        return mb.end();
    }

    public void equip() {
        if (state == WeaponState.HOLSTERED) {
            state = WeaponState.EQUIPPING;
            stateTimer = 0f;
        }
    }

    public void unequip() {
        if (state == WeaponState.IDLE || state == WeaponState.FIRING) {
            state = WeaponState.UNEQUIPPING;
            stateTimer = 0f;
        }
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
        bullets.add(new Bullet(bi, fireDir));

        Vector3 ejPos = new Vector3(muzzleWorldPos).add(rightDir.x * 0.05f, 0.02f, rightDir.z * 0.05f);
        ModelInstance ci = new ModelInstance(casingModel);
        ci.transform.setToTranslation(ejPos);
        casings.add(new Casing(ci, rightDir));

        showMuzzleFlash = true;
        muzzleFlashTimer = 0.055f;
        muzzleFlashInstance.transform.setToTranslation(
                muzzleWorldPos.x + fireDir.x * 0.04f,
                muzzleWorldPos.y + fireDir.y * 0.04f,
                muzzleWorldPos.z + fireDir.z * 0.04f);
    }

    public void triggerReload() {
        if (state == WeaponState.IDLE && ammoInMag < MAG_SIZE && ammoReserve > 0) {
            state = WeaponState.RELOADING;
            stateTimer = 0f;
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
                float dropY = rp < 0.3f ? MathUtils.sin(rp / 0.3f * MathUtils.PI) * -0.08f : 0f;
                currentPos.set(equipPos).add(0f, dropY, 0f);
                if (rp > 0.35f && rp < 0.65f) {
                    float insertP = (rp - 0.35f) / 0.3f;
                    currentPos.add(0f, MathUtils.sin(insertP * MathUtils.PI) * 0.06f, 0f);
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
                float breathX = MathUtils.sin(stateTimer * 1.8f) * 0.003f;
                float breathY = MathUtils.sin(stateTimer * 1.1f) * 0.002f;
                currentPos.set(equipPos).add(breathX, breathY - recoilPitch * 0.004f, recoilPitch * 0.003f);
                break;

            default:
                currentPos.set(holsterPos);
                currentRot.set(holsterRot);
                break;
        }

        if (thirdPerson && armRNode != null) {
            Matrix4 armWorld = armRNode.globalTransform.cpy().mulLeft(charTransform);
            akInstance.transform.set(armWorld);
            akInstance.transform.translate(currentPos);
            akInstance.transform.rotate(currentRot);
        } else {
            float rad = (float) Math.toRadians(yaw);
            Vector3 wp = new Vector3(
                    playerPos.x + (float)Math.sin(rad) * currentPos.z - (float)Math.cos(rad) * currentPos.x,
                    playerPos.y + currentPos.y,
                    playerPos.z + (float)Math.cos(rad) * currentPos.z + (float)Math.sin(rad) * currentPos.x
            );
            akInstance.transform.idt();
            akInstance.transform.setToTranslation(wp);
            akInstance.transform.rotate(Vector3.Y, yaw);
            akInstance.transform.rotate(currentRot);
        }

        applyTransform();

        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.life += delta;
            if (bullet.life >= Bullet.MAX_LIFE) {
                bullets.removeIndex(i);
                continue;
            }
            bullet.instance.transform.translate(
                    bullet.velocity.x * delta,
                    bullet.velocity.y * delta,
                    bullet.velocity.z * delta);
        }

        for (int i = casings.size - 1; i >= 0; i--) {
            Casing casing = casings.get(i);
            casing.life += delta;
            if (casing.life >= Casing.MAX_LIFE) {
                casings.removeIndex(i);
                continue;
            }
            casing.velocity.y -= 9.8f * delta;
            casing.instance.transform.translate(
                    casing.velocity.x * delta,
                    casing.velocity.y * delta,
                    casing.velocity.z * delta);
            casing.instance.transform.rotate(Vector3.X, casing.rotX * delta);
            casing.instance.transform.rotate(Vector3.Z, casing.rotZ * delta);
        }

        if (showMuzzleFlash) {
            muzzleFlashTimer -= delta;
            if (muzzleFlashTimer <= 0f) showMuzzleFlash = false;
        }
    }

    private void applyTransform() {
        Node root = akInstance.getNode("ak_root");
        if (root != null) {
            root.translation.set(0f, 0f, 0f);
            akInstance.calculateTransforms();
        }
    }

    public void render(ModelBatch batch, Environment env) {
        if (state != WeaponState.HOLSTERED) {
            batch.render(akInstance, env);
        }
        if (showMuzzleFlash) batch.render(muzzleFlashInstance, env);
        for (Bullet b : bullets) batch.render(b.instance, env);
        for (Casing c : casings) batch.render(c.instance, env);
    }

    public boolean isHolstered() { return state == WeaponState.HOLSTERED; }
    public boolean isReloading() { return state == WeaponState.RELOADING; }
    public boolean canFire() { return (state == WeaponState.IDLE || state == WeaponState.FIRING) && fireTimer <= 0f && ammoInMag > 0; }
    public float getRecoilPitch() { return recoilPitch; }
    public int getAmmoInMag() { return ammoInMag; }
    public int getAmmoReserve() { return ammoReserve; }

    @Override
    public void dispose() {
        akModel.dispose();
        bulletModel.dispose();
        casingModel.dispose();
        muzzleFlashModel.dispose();
    }
}