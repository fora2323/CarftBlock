package org.carft.block.weapon;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
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
    public static final float RELOAD_TIME = 2.2f;
    private static final float RECOIL_KICK = 6f;
    private static final float RECOIL_RECOVER = 12f;

    public int ammoInMag = MAG_SIZE;
    public int ammoReserve = 90;
    public WeaponState state = WeaponState.HOLSTERED;

    private final Model akModel;
    private final Model magModel;
    private final Model bulletModel;
    private final Model casingModel;
    private final Model muzzleFlashModel;
    private final Model fpsArmsModel;

    public final ModelInstance akInstance;
    public final ModelInstance magInstance;
    public final ModelInstance fpsArmsInstance;
    private ModelInstance muzzleFlashInstance;
    private boolean showMuzzleFlash = false;
    private float muzzleFlashTimer = 0f;

    private Sound shootSound;
    private Sound reloadSound;

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

    public float stateTimer = 0f;
    private float fireTimer = 0f;
    private float recoilPitch = 0f;
    private float sprintAnimTimer = 0f;

    private final Vector3 holsterPos = new Vector3(0.18f, 0.92f, 0.05f);
    private final Vector3 equipPos = new Vector3(0.24f, -0.24f, -0.42f);
    private final Vector3 currentPos = new Vector3();
    private final Quaternion currentRot = new Quaternion();
    private final Quaternion holsterRot = new Quaternion().setEulerAngles(-90f, 15f, 0f);
    private final Quaternion equipRot = new Quaternion().setEulerAngles(0f, 0f, 0f);

    public AK47() {
        try {
            shootSound = Gdx.audio.newSound(Gdx.files.internal("sound/ak47/ak47_shoot.ogg"));
            reloadSound = Gdx.audio.newSound(Gdx.files.internal("sound/realod/rifle_cock_away_heavy.ogg"));
        } catch (Exception e) {
            Gdx.app.log("AK47", "Gagal memuat suara.");
        }

        akModel = buildAkModel();
        magModel = buildMagModel();
        bulletModel = buildBulletModel();
        casingModel = buildCasingModel();
        muzzleFlashModel = buildMuzzleFlashModel();
        fpsArmsModel = buildFpsArmsModel();

        akInstance = new ModelInstance(akModel);
        magInstance = new ModelInstance(magModel);
        fpsArmsInstance = new ModelInstance(fpsArmsModel);
        muzzleFlashInstance = new ModelInstance(muzzleFlashModel);

        currentPos.set(equipPos);
        currentRot.set(equipRot);
    }

    private Model buildAkModel() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        Material metalDark = new Material(ColorAttribute.createDiffuse(new Color(0.12f, 0.12f, 0.12f, 1f)));
        Material metalMid = new Material(ColorAttribute.createDiffuse(new Color(0.20f, 0.20f, 0.20f, 1f)));
        Material wood = new Material(ColorAttribute.createDiffuse(new Color(0.48f, 0.28f, 0.12f, 1f)));

        Node root = mb.node(); root.id = "ak_root";

        mb.part("receiver", GL20.GL_TRIANGLES, attr, metalDark);
        BoxShapeBuilder.build(mb.part("receiver", GL20.GL_TRIANGLES, attr, metalDark),
                0f, 0f, 0f, 0.045f, 0.075f, 0.35f);

        mb.part("barrel", GL20.GL_TRIANGLES, attr, metalMid);
        BoxShapeBuilder.build(mb.part("barrel", GL20.GL_TRIANGLES, attr, metalMid),
                0f, 0.01f, -0.28f, 0.02f, 0.02f, 0.35f);

        mb.part("gastube", GL20.GL_TRIANGLES, attr, metalDark);
        BoxShapeBuilder.build(mb.part("gastube", GL20.GL_TRIANGLES, attr, metalDark),
                0f, 0.038f, -0.22f, 0.015f, 0.015f, 0.18f);

        mb.part("handguard", GL20.GL_TRIANGLES, attr, wood);
        BoxShapeBuilder.build(mb.part("handguard", GL20.GL_TRIANGLES, attr, wood),
                0f, -0.005f, -0.21f, 0.035f, 0.045f, 0.16f);

        MeshPartBuilder stockBuilder = mb.part("stock", GL20.GL_TRIANGLES, attr, wood);
        stockBuilder.setVertexTransform(new Matrix4().setToTranslation(0f, -0.02f, 0.26f).rotate(Vector3.X, -8f));
        BoxShapeBuilder.build(stockBuilder, 0f, 0f, 0f, 0.035f, 0.065f, 0.22f);

        MeshPartBuilder gripBuilder = mb.part("grip", GL20.GL_TRIANGLES, attr, wood);
        gripBuilder.setVertexTransform(new Matrix4().setToTranslation(0f, -0.08f, 0.12f).rotate(Vector3.X, 15f));
        BoxShapeBuilder.build(gripBuilder, 0f, 0f, 0f, 0.032f, 0.11f, 0.045f);

        mb.part("frontsight", GL20.GL_TRIANGLES, attr, metalDark);
        BoxShapeBuilder.build(mb.part("frontsight", GL20.GL_TRIANGLES, attr, metalDark),
                0f, 0.035f, -0.42f, 0.008f, 0.03f, 0.015f);

        Model model = mb.end();
        model.nodes.removeValue(root, true);
        Node fakeRoot = new Node(); fakeRoot.id = "ak_root";
        fakeRoot.addChild(root);
        model.nodes.add(fakeRoot);
        return model;
    }

    private Model buildMagModel() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Material magazineMat = new Material(ColorAttribute.createDiffuse(new Color(0.15f, 0.15f, 0.15f, 1f)));
        Node root = mb.node(); root.id = "mag_root";
        MeshPartBuilder magBuilder = mb.part("mag", GL20.GL_TRIANGLES, attr, magazineMat);
        magBuilder.setVertexTransform(new Matrix4().rotate(Vector3.X, 15f));
        BoxShapeBuilder.build(magBuilder, 0f, 0f, 0f, 0.035f, 0.16f, 0.075f);
        return mb.end();
    }

    private Model buildFpsArmsModel() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Material skinMat = new Material(ColorAttribute.createDiffuse(new Color(0.92f, 0.74f, 0.60f, 1f)));
        Material sleeveMat = new Material(ColorAttribute.createDiffuse(new Color(0.18f, 0.52f, 0.82f, 1f)));

        Node root = mb.node(); root.id = "fps_arms";

        Node rArm = mb.node(); rArm.id = "fps_rArm";
        mb.part("r_sleeve", GL20.GL_TRIANGLES, attr, sleeveMat);
        BoxShapeBuilder.build(mb.part("r_sleeve", GL20.GL_TRIANGLES, attr, sleeveMat), 0f, 0f, 0.15f, 0.11f, 0.11f, 0.35f);
        mb.part("r_hand", GL20.GL_TRIANGLES, attr, skinMat);
        BoxShapeBuilder.build(mb.part("r_hand", GL20.GL_TRIANGLES, attr, skinMat), 0f, -0.02f, -0.05f, 0.08f, 0.09f, 0.14f);

        Node lArm = mb.node(); lArm.id = "fps_lArm";
        mb.part("l_sleeve", GL20.GL_TRIANGLES, attr, sleeveMat);
        BoxShapeBuilder.build(mb.part("l_sleeve", GL20.GL_TRIANGLES, attr, sleeveMat), 0f, 0f, 0.15f, 0.11f, 0.11f, 0.35f);
        mb.part("l_hand", GL20.GL_TRIANGLES, attr, skinMat);
        BoxShapeBuilder.build(mb.part("l_hand", GL20.GL_TRIANGLES, attr, skinMat), 0f, 0.01f, -0.05f, 0.08f, 0.08f, 0.14f);

        return mb.end();
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
        Material flash = new Material(ColorAttribute.createDiffuse(new Color(1f, 0.85f, 0.1f, 0.9f)));
        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Node r = mb.node(); r.id = "flash";
        BoxShapeBuilder.build(mb.part("f", GL20.GL_TRIANGLES, attr, flash),
                0f, 0f, 0f, 0.08f, 0.08f, 0.12f);
        return mb.end();
    }

    public void equip() {
        state = WeaponState.EQUIPPING;
        stateTimer = 0f;
    }

    public void triggerFire(Vector3 muzzleWorldPos, Vector3 fireDir, Vector3 rightDir) {
        if (state != WeaponState.IDLE && state != WeaponState.FIRING) return;
        if (ammoInMag <= 0 || fireTimer > 0f) return;

        ammoInMag--;
        fireTimer = FIRE_RATE;
        recoilPitch += RECOIL_KICK;
        state = WeaponState.FIRING;

        if (shootSound != null) shootSound.play(0.6f);

        ModelInstance bi = new ModelInstance(bulletModel);
        bi.transform.setToTranslation(muzzleWorldPos);
        bullets.add(new Bullet(bi, fireDir));

        Vector3 ejPos = new Vector3(muzzleWorldPos).add(rightDir.x * 0.05f, 0.02f, rightDir.z * 0.05f);
        ModelInstance ci = new ModelInstance(casingModel);
        ci.transform.setToTranslation(ejPos);
        casings.add(new Casing(ci, rightDir));

        showMuzzleFlash = true;
        muzzleFlashTimer = 0.05f;
        muzzleFlashInstance.transform.setToTranslation(
                muzzleWorldPos.x + fireDir.x * 0.04f,
                muzzleWorldPos.y + fireDir.y * 0.04f,
                muzzleWorldPos.z + fireDir.z * 0.04f);
    }

    public void triggerReload() {
        if (state != WeaponState.RELOADING && ammoInMag < MAG_SIZE && ammoReserve > 0) {
            state = WeaponState.RELOADING;
            stateTimer = 0f;
            if (reloadSound != null) reloadSound.play(0.85f);
        }
    }

    public void update(float delta, PerspectiveCamera camera, boolean thirdPerson,
                       Node armRNode, Matrix4 charTransform, boolean isSprinting, boolean isMoving) {
        
        if (fireTimer > 0f) fireTimer -= delta;
        recoilPitch = MathUtils.lerp(recoilPitch, 0f, Math.min(1f, delta * RECOIL_RECOVER));
        if (recoilPitch < 0.1f && state == WeaponState.FIRING) state = WeaponState.IDLE;

        stateTimer += delta;
        if (isMoving) sprintAnimTimer += delta * (isSprinting ? 12f : 7f);

        Vector3 targetPos = new Vector3(equipPos);
        float sprintRoll = 0f, sprintPitch = 0f, sprintYaw = 0f;

        // Tactical Sprint Bobbing & Rotation
        if (isSprinting && state == WeaponState.IDLE) {
            float bobX = MathUtils.sin(sprintAnimTimer) * 0.04f;
            float bobY = Math.abs(MathUtils.cos(sprintAnimTimer)) * 0.035f;
            targetPos.add(-0.06f + bobX, 0.04f + bobY, 0.08f);
            sprintPitch = -28f;
            sprintYaw = 18f;
            sprintRoll = -15f;
        } else if (isMoving && state == WeaponState.IDLE) {
            float walkSwayX = MathUtils.sin(sprintAnimTimer) * 0.012f;
            float walkSwayY = Math.abs(MathUtils.cos(sprintAnimTimer)) * 0.01f;
            targetPos.add(walkSwayX, walkSwayY, 0f);
        }

        switch (state) {
            case EQUIPPING:
                float ep = Math.min(1f, stateTimer / EQUIP_TIME);
                currentPos.set(holsterPos).lerp(targetPos, ep);
                currentRot.set(holsterRot).slerp(equipRot, ep);
                if (stateTimer >= EQUIP_TIME) state = WeaponState.IDLE;
                break;

            case RELOADING:
                float rp = stateTimer / RELOAD_TIME;
                // Posisi senjata ditarik ke dada saat reload
                float tiltZ = MathUtils.sin(rp * MathUtils.PI) * 15f;
                currentPos.set(targetPos).add(-0.05f, -0.06f, 0.05f);
                currentRot.set(equipRot).setEulerAngles(sprintYaw, sprintPitch - tiltZ, sprintRoll + tiltZ);

                if (stateTimer >= RELOAD_TIME) {
                    int needed = MAG_SIZE - ammoInMag;
                    int take = Math.min(needed, ammoReserve);
                    ammoInMag += take;
                    ammoReserve -= take;
                    state = WeaponState.IDLE;
                    currentPos.set(targetPos);
                    currentRot.set(equipRot);
                }
                break;

            case IDLE:
            case FIRING:
                float breathX = MathUtils.sin(stateTimer * 1.8f) * 0.002f;
                float breathY = MathUtils.sin(stateTimer * 1.1f) * 0.002f;
                currentPos.set(targetPos).add(breathX, breathY - recoilPitch * 0.004f, recoilPitch * 0.003f);
                currentRot.set(equipRot).setEulerAngles(sprintYaw, sprintPitch + recoilPitch * 3.5f, sprintRoll);
                break;

            default:
                break;
        }

        Vector3 magWorldPos = new Vector3();
        Quaternion magWorldRot = new Quaternion();

        if (thirdPerson && armRNode != null) {
            // TPP Weapon Placement (Kaki/Bahu tangan kanan memegang erat grip)
            Matrix4 armWorld = armRNode.globalTransform.cpy().mulLeft(charTransform);
            akInstance.transform.set(armWorld);
            akInstance.transform.translate(0.04f, -0.32f, -0.22f);
            akInstance.transform.rotate(Vector3.X, -82f);
            akInstance.transform.rotate(Vector3.Y, 10f);

            // TPP Magazine Logic
            magInstance.transform.set(akInstance.transform);
            if (state == WeaponState.RELOADING) {
                float rp = stateTimer / RELOAD_TIME;
                float drop = (rp < 0.45f) ? MathUtils.sin(rp / 0.45f * MathUtils.PI) * -0.4f : 0f;
                magInstance.transform.translate(0f, -0.07f + drop, 0.01f);
            } else {
                magInstance.transform.translate(0f, -0.07f, 0.01f);
            }

        } else {
            // FPS Camera Attachment
            akInstance.transform.idt();
            akInstance.transform.setToLookAt(camera.direction, camera.up).inv();
            akInstance.transform.setTranslation(camera.position);
            akInstance.transform.translate(currentPos);
            akInstance.transform.rotate(currentRot);

            // FPS Magazine Transform (Bergerak keluar masuk saat reload)
            magInstance.transform.set(akInstance.transform);
            float magOffsetY = -0.07f;
            float magOffsetZ = 0.01f;

            if (state == WeaponState.RELOADING) {
                float rp = stateTimer / RELOAD_TIME;
                if (rp < 0.42f) { // Lepas magazin ke bawah
                    magOffsetY -= (rp / 0.42f) * 0.35f;
                } else if (rp < 0.75f) { // Ambil dan masukkan magazin baru
                    float ins = (rp - 0.42f) / 0.33f;
                    magOffsetY -= (1f - ins) * 0.35f;
                }
            }
            magInstance.transform.translate(0f, magOffsetY, magOffsetZ);

            // FPS Arms Attachment
            fpsArmsInstance.transform.set(akInstance.transform);
            Node rArm = fpsArmsInstance.getNode("fps_rArm");
            Node lArm = fpsArmsInstance.getNode("fps_lArm");

            if (rArm != null) {
                // Tangan Kanan selalu mengunci di Grip & Pelatuk
                rArm.translation.set(0.02f, -0.08f, 0.16f);
                rArm.rotation.setEulerAngles(0f, 15f, 0f);
            }
            if (lArm != null) {
                if (state == WeaponState.RELOADING) {
                    float rp = stateTimer / RELOAD_TIME;
                    if (rp < 0.42f) { // Tangan kiri menarik magazin turun
                        lArm.translation.set(0f, magOffsetY, magOffsetZ);
                        lArm.rotation.setEulerAngles(0f, 30f, -20f);
                    } else if (rp < 0.75f) { // Tangan kiri mendorong magazin masuk
                        lArm.translation.set(0f, magOffsetY, magOffsetZ);
                        lArm.rotation.setEulerAngles(0f, 25f, -10f);
                    } else { // Mengokang charging handle di kanan atas
                        float cock = (rp - 0.75f) / 0.25f;
                        float slideBack = MathUtils.sin(cock * MathUtils.PI) * 0.12f;
                        lArm.translation.set(0.06f, 0.05f, 0.08f + slideBack);
                        lArm.rotation.setEulerAngles(0f, -40f, 35f);
                    }
                } else {
                    // Tangan Kiri menopang Handguard laras depan
                    lArm.translation.set(-0.02f, -0.01f, -0.19f);
                    lArm.rotation.setEulerAngles(0f, -25f, 20f);
                }
            }
            fpsArmsInstance.calculateTransforms();
        }

        // Update Projectiles
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

    public void render(ModelBatch batch, Environment env, boolean thirdPerson) {
        if (state != WeaponState.HOLSTERED) {
            batch.render(akInstance, env);
            batch.render(magInstance, env);
            if (!thirdPerson) {
                batch.render(fpsArmsInstance, env); // Render tangan khusus FPS
            }
        }
        if (showMuzzleFlash) batch.render(muzzleFlashInstance, env);
        for (Bullet b : bullets) batch.render(b.instance, env);
        for (Casing c : casings) batch.render(c.instance, env);
    }

    public boolean isReloading() { return state == WeaponState.RELOADING; }
    public boolean canFire() { return (state == WeaponState.IDLE || state == WeaponState.FIRING) && fireTimer <= 0f && ammoInMag > 0; }
    public int getAmmoInMag() { return ammoInMag; }
    public int getAmmoReserve() { return ammoReserve; }

    @Override
    public void dispose() {
        akModel.dispose();
        magModel.dispose();
        bulletModel.dispose();
        casingModel.dispose();
        muzzleFlashModel.dispose();
        fpsArmsModel.dispose();
        if (shootSound != null) shootSound.dispose();
        if (reloadSound != null) reloadSound.dispose();
    }
}