package org.carft.block.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import org.carft.block.world.VoxelArena;

public class Hud implements Disposable {
    public interface HudListener {
        void onJumpPressed();
        void onCrouchToggled();
        void onProneToggled();
        void onScopeToggled(boolean active);
        void onWeaponSelected(int index);
        void onGrenadeUsed();
        void onMedkitUsed();
        void onCameraViewToggled();
        void onExitToHome();
    }

    public final Stage stage;
    public Touchpad touchpad;
    public TextButton btnFireLeft, btnFireRight;
    public TextButton btnJump, btnCrouch, btnProne, btnScope;
    public TextButton btnWeapon1, btnWeapon2;
    public TextButton btnGrenade, btnMedkit;
    public TextButton btnMap, btnSettings, btnCamView;

    private ProgressBar hpBar, armorBar;
    private Label lblHpVal, lblArmorVal, lblAmmoInfo, lblDiveHint;
    private Table pauseModal, mapModal;
    private Image mapImage;
    private Pixmap mapPixmap;
    private Texture mapTexture;

    private final Skin skin;
    private final HudListener listener;
    private final VoxelArena arena;

    private int health = 100;
    private int armor = 100;
    private int grenadeCount = 3;
    private int medkitCount = 2;
    private boolean paused = false;
    private boolean mapOpen = false;

    public Hud(UiAssets assets, VoxelArena arena, HudListener listener) {
        this.skin = assets.skin;
        this.arena = arena;
        this.listener = listener;
        this.stage = new Stage(new ExtendViewport(1280, 720));

        buildMainHud(assets);
        buildMapModal();
        buildPauseModal();
    }

    private void buildMainHud(UiAssets assets) {
        // Crosshair tepat di tengah layar
        Image crosshair = new Image(assets.crosshairTex);
        crosshair.setSize(28, 28);
        crosshair.setPosition(1280 / 2f - 14f, 720 / 2f - 14f);
        stage.addActor(crosshair);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // 1. BARIS ATAS: HP/Armor di kiri, Tombol CAM / PETA / SETTINGS di kanan
        Table statusTable = new Table();
        hpBar = new ProgressBar(0, 100, 1, false, skin, "hp-bar");
        hpBar.setValue(health);
        lblHpVal = new Label("HP 100", skin);

        armorBar = new ProgressBar(0, 100, 1, false, skin, "armor-bar");
        armorBar.setValue(armor);
        lblArmorVal = new Label("ARM 100", skin);

        statusTable.add(lblHpVal).width(95).left();
        statusTable.add(hpBar).width(190).height(20).padLeft(8).row();
        statusTable.add(lblArmorVal).width(95).left().padTop(6);
        statusTable.add(armorBar).width(190).height(20).padLeft(8).padTop(6);

        lblAmmoInfo = new Label("RIFLE | 30/30", skin, "title");
        lblDiveHint = new Label("Tip: Lari + Lompat + Klik TIARAP = Lompat Telentang!", skin);
        lblDiveHint.setFontScale(0.95f);

        btnCamView = new TextButton("CAM: 3RD", skin);
        btnMap = new TextButton("PETA", skin);
        btnSettings = new TextButton("SET", skin);

        Table topRight = new Table();
        topRight.add(btnCamView).size(125, 64).padRight(10);
        topRight.add(btnMap).size(100, 64).padRight(10);
        topRight.add(btnSettings).size(90, 64);

        Table topBar = new Table();
        topBar.add(statusTable).left().pad(14);
        Table centerInfo = new Table();
        centerInfo.add(lblAmmoInfo).row();
        centerInfo.add(lblDiveHint).padTop(4);
        topBar.add(centerInfo).expandX().center();
        topBar.add(topRight).right().pad(14);

        // 2. SISI KIRI: Granat, Medkit, Fire-Kiri, Analog
        btnGrenade = new TextButton("BOM (3)", skin);
        btnMedkit = new TextButton("MED (2)", skin);
        btnFireLeft = new TextButton("FIRE", skin, "fire");
        touchpad = new Touchpad(8f, skin);

        Table leftItems = new Table();
        leftItems.add(btnGrenade).size(115, 65).padBottom(8).row();
        leftItems.add(btnMedkit).size(115, 65).padBottom(12).row();
        leftItems.add(btnFireLeft).size(115, 95);

        Table leftBottom = new Table();
        leftBottom.add(leftItems).padLeft(18).padBottom(14).bottom();
        leftBottom.add(touchpad).size(200, 200).padLeft(18).padBottom(14);

        // 3. TENGAH BAWAH: Pilihan Senjata 1 & 2
        btnWeapon1 = new TextButton("1. RIFLE", skin, "toggle");
        btnWeapon1.setChecked(true);
        btnWeapon2 = new TextButton("2. PISTOL", skin, "toggle");

        Table weaponBar = new Table();
        weaponBar.add(btnWeapon1).size(155, 68).padRight(12);
        weaponBar.add(btnWeapon2).size(155, 68);

        // 4. SISI KANAN: Scope, Tiarap, Jongkok, Loncat, Fire-Kanan
        btnScope = new TextButton("SCOPE", skin, "toggle");
        btnProne = new TextButton("TIARAP", skin, "toggle");
        btnCrouch = new TextButton("JONGKOK", skin, "toggle");
        btnJump = new TextButton("LONCAT", skin);
        btnFireRight = new TextButton("FIRE", skin, "fire");

        Table stanceRow = new Table();
        stanceRow.add(btnProne).size(110, 72).pad(5);
        stanceRow.add(btnCrouch).size(115, 72).pad(5);
        stanceRow.add(btnJump).size(110, 82).pad(5);

        Table rightBottom = new Table();
        rightBottom.add(btnScope).size(115, 72).colspan(2).right().padRight(16).padBottom(8).row();
        rightBottom.add(btnFireRight).size(125, 115).padRight(12).bottom();
        rightBottom.add(stanceRow).right().padRight(14).bottom();

        // Susun semua ke Root Table
        root.add(topBar).colspan(3).expandX().fillX().top().row();
        root.add().colspan(3).expand().fill().row();
        root.add(leftBottom).left().bottom();
        root.add(weaponBar).bottom().padBottom(16);
        root.add(rightBottom).right().bottom();

        registerListeners();
    }

    private void buildMapModal() {
        mapPixmap = new Pixmap(arena.width * 4, arena.depth * 4, Pixmap.Format.RGBA8888);
        mapTexture = new Texture(mapPixmap);
        mapImage = new Image(new TextureRegionDrawable(mapTexture));

        mapModal = new Table(skin);
        mapModal.setFillParent(true);
        mapModal.setVisible(false);

        Table box = new Table(skin);
        box.setBackground("panel-dark");
        box.pad(20);

        Label title = new Label("PETA ARENA (64x64)", skin, "title");
        TextButton btnCloseMap = new TextButton("TUTUP (X)", skin);

        box.add(title).padBottom(12).row();
        box.add(mapImage).size(360, 360).padBottom(14).row();
        box.add(btnCloseMap).size(200, 64);

        mapModal.add(box);
        stage.addActor(mapModal);

        btnCloseMap.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                mapOpen = false;
                mapModal.setVisible(false);
            }
        });
    }

    private void buildPauseModal() {
        pauseModal = new Table(skin);
        pauseModal.setFillParent(true);
        pauseModal.setVisible(false);

        Table box = new Table(skin);
        box.setBackground("panel-dark");
        box.pad(36);

        Label title = new Label("GAME DIJEDA", skin, "title");
        TextButton btnResume = new TextButton("LANJUTKAN", skin);
        TextButton btnExit = new TextButton("EXIT KE HOME", skin);

        box.add(title).padBottom(30).row();
        box.add(btnResume).size(280, 75).padBottom(16).row();
        box.add(btnExit).size(280, 75);

        pauseModal.add(box);
        stage.addActor(pauseModal);

        btnResume.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                paused = false;
                pauseModal.setVisible(false);
            }
        });

        btnExit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                listener.onExitToHome();
            }
        });
    }

    private void registerListeners() {
        btnJump.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!isOverlayOpen()) listener.onJumpPressed();
            }
        });

        btnCrouch.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!isOverlayOpen()) listener.onCrouchToggled();
            }
        });

        btnProne.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!isOverlayOpen()) listener.onProneToggled();
            }
        });

        btnScope.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!isOverlayOpen()) listener.onScopeToggled(btnScope.isChecked());
            }
        });

        btnWeapon1.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (btnWeapon1.isChecked()) {
                    btnWeapon2.setChecked(false);
                    listener.onWeaponSelected(0);
                } else if (!btnWeapon2.isChecked()) {
                    btnWeapon1.setChecked(true);
                }
            }
        });

        btnWeapon2.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (btnWeapon2.isChecked()) {
                    btnWeapon1.setChecked(false);
                    listener.onWeaponSelected(1);
                } else if (!btnWeapon1.isChecked()) {
                    btnWeapon2.setChecked(true);
                }
            }
        });

        btnGrenade.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!isOverlayOpen() && grenadeCount > 0) {
                    grenadeCount--;
                    btnGrenade.setText("BOM (" + grenadeCount + ")");
                    if (grenadeCount == 0) btnGrenade.setDisabled(true);
                    listener.onGrenadeUsed();
                }
            }
        });

        btnMedkit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!isOverlayOpen() && medkitCount > 0 && health < 100) {
                    medkitCount--;
                    setHealth(Math.min(100, health + 50));
                    btnMedkit.setText("MED (" + medkitCount + ")");
                    if (medkitCount == 0) btnMedkit.setDisabled(true);
                    listener.onMedkitUsed();
                }
            }
        });

        btnCamView.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                listener.onCameraViewToggled();
            }
        });

        btnMap.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!paused) {
                    mapOpen = !mapOpen;
                    mapModal.setVisible(mapOpen);
                }
            }
        });

        btnSettings.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                paused = !paused;
                pauseModal.setVisible(paused);
                if (paused) {
                    mapOpen = false;
                    mapModal.setVisible(false);
                }
            }
        });
    }

    // Menggambar peta top-down 64x64 dari VoxelArena + posisi dan arah pemain
    public void refreshTopDownMap(float playerX, float playerZ, float yawDeg) {
        if (!mapOpen) return;
        int scale = 4;
        for (int x = 0; x < arena.width; x++) {
            for (int z = 0; z < arena.depth; z++) {
                if (arena.getBlock(x, 1, z) == VoxelArena.WALL) {
                    mapPixmap.setColor(0.45f, 0.28f, 0.15f, 1f);
                } else if (arena.getBlock(x, 1, z) == VoxelArena.OBSTACLE) {
                    mapPixmap.setColor(0.35f, 0.55f, 0.25f, 1f);
                } else {
                    mapPixmap.setColor(0.25f, 0.28f, 0.30f, 1f);
                }
                mapPixmap.fillRectangle(x * scale, z * scale, scale, scale);
            }
        }
        int px = (int) (playerX * scale);
        int pz = (int) (playerZ * scale);
        mapPixmap.setColor(Color.RED);
        mapPixmap.fillCircle(px, pz, 5);

        float rad = (float) Math.toRadians(yawDeg);
        int dirX = px + (int) (Math.sin(rad) * 12f);
        int dirZ = pz + (int) (Math.cos(rad) * 12f);
        mapPixmap.setColor(Color.YELLOW);
        mapPixmap.drawLine(px, pz, dirX, dirZ);

        mapTexture.draw(mapPixmap, 0, 0);
    }

    public void syncStanceButtons(int stance) {
        btnCrouch.setProgrammaticChangeEvents(false);
        btnProne.setProgrammaticChangeEvents(false);
        btnCrouch.setChecked(stance == 1);
        btnProne.setChecked(stance == 2);
        btnCrouch.setProgrammaticChangeEvents(true);
        btnProne.setProgrammaticChangeEvents(true);
    }

    public void setHealth(int hp) {
        this.health = Math.max(0, Math.min(100, hp));
        hpBar.setValue(this.health);
        lblHpVal.setText("HP " + this.health);
    }

    public void setArmor(int arm) {
        this.armor = Math.max(0, Math.min(100, arm));
        armorBar.setValue(this.armor);
        lblArmorVal.setText("ARM " + this.armor);
    }

    public void setWeaponStatus(String text) {
        lblAmmoInfo.setText(text);
    }

    public void setCameraButtonLabel(boolean thirdPerson) {
        btnCamView.setText(thirdPerson ? "CAM: 3RD" : "CAM: 1ST");
    }

    public boolean isFiringPressed() {
        return !isOverlayOpen() && (btnFireLeft.isPressed() || btnFireRight.isPressed());
    }

    public boolean isOverlayOpen() {
        return paused || mapOpen;
    }

    public boolean isPaused() {
        return paused;
    }

    public void update(float delta) {
        stage.act(delta);
    }

    public void render() {
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
        mapPixmap.dispose();
        mapTexture.dispose();
    }
}