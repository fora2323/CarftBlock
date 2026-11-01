package org.carft.block.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

public class Hud implements Disposable {
    public Stage stage;
    public Touchpad touchpad;
    public TextButton btnFireLeft, btnFireRight, btnJump, btnCrouch, btnProne, btnScope;
    public TextButton btnSettings, btnMap;
    public ProgressBar hpBar, armorBar;

    private Skin skin;
    private Array<Texture> genTextures = new Array<>();

    public Hud() {
        stage = new Stage(new ExtendViewport(1280, 720));
        skin = new Skin();

        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.2f);
        skin.add("default-font", font);

        setupGeneratedAssets();
        buildUI();
    }

    private void setupGeneratedAssets() {
        // Tombol Biasa
        Pixmap pixBtn = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixBtn.setColor(new Color(0.5f, 0.5f, 0.5f, 0.7f));
        pixBtn.fill();
        Texture texBtn = new Texture(pixBtn);
        genTextures.add(texBtn);
        skin.add("btn-up", new TextureRegionDrawable(texBtn));
        pixBtn.dispose();

        // Tombol Ditekan
        Pixmap pixBtnDown = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixBtnDown.setColor(new Color(0.8f, 0.2f, 0.2f, 0.8f));
        pixBtnDown.fill();
        Texture texBtnDown = new Texture(pixBtnDown);
        genTextures.add(texBtnDown);
        skin.add("btn-down", new TextureRegionDrawable(texBtnDown));
        pixBtnDown.dispose();

        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.up = skin.getDrawable("btn-up");
        btnStyle.down = skin.getDrawable("btn-down");
        btnStyle.font = skin.getFont("default-font");
        skin.add("default", btnStyle);

        // Joystick (Touchpad)
        Pixmap bg = new Pixmap(200, 200, Pixmap.Format.RGBA8888);
        bg.setColor(new Color(1, 1, 1, 0.2f));
        bg.fillCircle(100, 100, 100);
        Texture bgTex = new Texture(bg);
        genTextures.add(bgTex);
        bg.dispose();

        Pixmap knob = new Pixmap(60, 60, Pixmap.Format.RGBA8888);
        knob.setColor(new Color(1, 1, 1, 0.6f));
        knob.fillCircle(30, 30, 30);
        Texture knobTex = new Texture(knob);
        genTextures.add(knobTex);
        knob.dispose();

        Touchpad.TouchpadStyle padStyle = new Touchpad.TouchpadStyle();
        padStyle.background = new TextureRegionDrawable(bgTex);
        padStyle.knob = new TextureRegionDrawable(knobTex);
        skin.add("default", padStyle);
    }

    private void buildUI() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // KIRI BAWAH: Analog & Fire Kiri
        touchpad = new Touchpad(10f, skin);
        btnFireLeft = new TextButton("FIRE", skin);

        Table leftBottom = new Table();
        leftBottom.add(touchpad).size(200, 200).pad(20);
        leftBottom.add(btnFireLeft).size(100, 100).padBottom(40).bottom();

        // KANAN BAWAH: Aksi & Fire Kanan
        btnFireRight = new TextButton("FIRE", skin);
        btnJump = new TextButton("JUMP", skin);
        btnCrouch = new TextButton("CROUCH", skin);
        btnProne = new TextButton("PRONE", skin);
        btnScope = new TextButton("SCOPE", skin);

        Table actionGroup = new Table();
        actionGroup.add(btnScope).size(90, 90).pad(10).colspan(2).right().row();
        actionGroup.add(btnProne).size(80, 80).pad(5);
        actionGroup.add(btnCrouch).size(80, 80).pad(5);
        actionGroup.add(btnJump).size(90, 90).pad(5).row();
        
        Table rightBottom = new Table();
        rightBottom.add(btnFireRight).size(120, 120).padRight(20);
        rightBottom.add(actionGroup).pad(20);

        // ATAS: Indikator & Menu
        Label hpLabel = new Label("HP: 100", new Label.LabelStyle(skin.getFont("default-font"), Color.GREEN));
        btnSettings = new TextButton("SET", skin);
        btnMap = new TextButton("MAP", skin);

        Table topBar = new Table();
        topBar.add(hpLabel).left().pad(20).expandX();
        topBar.add(btnMap).size(80, 60).pad(10);
        topBar.add(btnSettings).size(80, 60).pad(10);

        // Susun ke Root Table
        root.add(topBar).expandX().fillX().top().colspan(2).row();
        root.add().expand().fill().colspan(2).row(); // Ruang kosong tengah
        root.add(leftBottom).left().bottom();
        root.add(rightBottom).right().bottom();
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
        skin.dispose();
        for (Texture t : genTextures) t.dispose();
    }
}