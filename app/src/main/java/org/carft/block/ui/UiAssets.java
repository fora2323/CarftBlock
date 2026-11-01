package org.carft.block.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad.TouchpadStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class UiAssets implements Disposable {
    public Skin skin;
    public BitmapFont font;
    public Texture crosshairTex;
    private final Array<Texture> textures = new Array<>();

    public UiAssets() {
        skin = new Skin();
        font = new BitmapFont();
        font.getData().setScale(1.35f);
        skin.add("default-font", font);

        createBaseStyles();
        createHudIcons();
    }

    private Texture registerTexture(Pixmap pixmap) {
        Texture tex = new Texture(pixmap);
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        textures.add(tex);
        pixmap.dispose();
        return tex;
    }

    private Drawable createBevelBox(int w, int h, Color fill, Color topBevel, Color bottomBevel, Color border) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(border);
        p.fill();
        p.setColor(fill);
        p.fillRectangle(2, 2, w - 4, h - 4);
        p.setColor(topBevel);
        p.fillRectangle(2, 2, w - 4, 4);
        p.fillRectangle(2, 2, 4, h - 4);
        p.setColor(bottomBevel);
        p.fillRectangle(2, h - 6, w - 4, 4);
        p.fillRectangle(w - 6, 2, 4, h - 4);
        return new TextureRegionDrawable(registerTexture(p));
    }

    private void createBaseStyles() {
        Drawable btnUp = createBevelBox(64, 64,
                new Color(0.45f, 0.45f, 0.45f, 0.9f),
                new Color(0.75f, 0.75f, 0.75f, 0.9f),
                new Color(0.22f, 0.22f, 0.22f, 0.9f),
                Color.BLACK);

        Drawable btnDown = createBevelBox(64, 64,
                new Color(0.28f, 0.35f, 0.28f, 0.95f),
                new Color(0.18f, 0.22f, 0.18f, 0.95f),
                new Color(0.55f, 0.65f, 0.55f, 0.95f),
                Color.BLACK);

        Drawable btnActive = createBevelBox(64, 64,
                new Color(0.2f, 0.55f, 0.25f, 0.95f),
                new Color(0.4f, 0.85f, 0.45f, 0.95f),
                new Color(0.1f, 0.35f, 0.15f, 0.95f),
                Color.YELLOW);

        Drawable btnFireUp = createBevelBox(64, 64,
                new Color(0.7f, 0.2f, 0.15f, 0.85f),
                new Color(0.95f, 0.4f, 0.35f, 0.85f),
                new Color(0.45f, 0.1f, 0.08f, 0.85f),
                Color.BLACK);

        Drawable btnFireDown = createBevelBox(64, 64,
                new Color(0.95f, 0.35f, 0.1f, 0.95f),
                new Color(0.5f, 0.15f, 0.05f, 0.95f),
                new Color(1f, 0.6f, 0.2f, 0.95f),
                Color.YELLOW);

        skin.add("panel-dark", createBevelBox(32, 32,
                new Color(0.1f, 0.12f, 0.15f, 0.88f),
                new Color(0.25f, 0.28f, 0.32f, 0.88f),
                new Color(0.05f, 0.06f, 0.08f, 0.88f),
                Color.BLACK), Drawable.class);

        TextButtonStyle defaultBtn = new TextButtonStyle(btnUp, btnDown, btnUp, font);
        defaultBtn.fontColor = Color.WHITE;
        defaultBtn.disabledFontColor = Color.GRAY;
        skin.add("default", defaultBtn);

        TextButtonStyle toggleBtn = new TextButtonStyle(btnUp, btnDown, btnActive, font);
        toggleBtn.fontColor = Color.WHITE;
        toggleBtn.checkedFontColor = Color.YELLOW;
        skin.add("toggle", toggleBtn);

        TextButtonStyle fireBtn = new TextButtonStyle(btnFireUp, btnFireDown, btnFireUp, font);
        fireBtn.fontColor = Color.WHITE;
        skin.add("fire", fireBtn);

        // Slider
        Pixmap sBg = new Pixmap(120, 14, Pixmap.Format.RGBA8888);
        sBg.setColor(Color.BLACK);
        sBg.fill();
        sBg.setColor(Color.DARK_GRAY);
        sBg.fillRectangle(2, 2, 116, 10);

        Pixmap sKnob = new Pixmap(22, 32, Pixmap.Format.RGBA8888);
        sKnob.setColor(Color.BLACK);
        sKnob.fill();
        sKnob.setColor(Color.LIGHT_GRAY);
        sKnob.fillRectangle(2, 2, 18, 28);

        SliderStyle sliderStyle = new SliderStyle(
                new TextureRegionDrawable(registerTexture(sBg)),
                new TextureRegionDrawable(registerTexture(sKnob)));
        skin.add("default-horizontal", sliderStyle);

        // ProgressBar (HP & Armor)
        Pixmap barBg = new Pixmap(100, 18, Pixmap.Format.RGBA8888);
        barBg.setColor(new Color(0.1f, 0.1f, 0.1f, 0.85f));
        barBg.fill();
        Drawable dBarBg = new TextureRegionDrawable(registerTexture(barBg));

        Pixmap hpFill = new Pixmap(10, 14, Pixmap.Format.RGBA8888);
        hpFill.setColor(new Color(0.2f, 0.85f, 0.25f, 1f));
        hpFill.fill();
        ProgressBarStyle hpStyle = new ProgressBarStyle(dBarBg, null);
        hpStyle.knobBefore = new TextureRegionDrawable(registerTexture(hpFill));
        skin.add("hp-bar", hpStyle);

        Pixmap armorFill = new Pixmap(10, 14, Pixmap.Format.RGBA8888);
        armorFill.setColor(new Color(0.25f, 0.65f, 0.95f, 1f));
        armorFill.fill();
        ProgressBarStyle armorStyle = new ProgressBarStyle(dBarBg, null);
        armorStyle.knobBefore = new TextureRegionDrawable(registerTexture(armorFill));
        skin.add("armor-bar", armorStyle);

        // Touchpad (Analog)
        Pixmap padBg = new Pixmap(200, 200, Pixmap.Format.RGBA8888);
        padBg.setColor(new Color(0f, 0f, 0f, 0.35f));
        padBg.fillCircle(100, 100, 94);
        padBg.setColor(new Color(1f, 1f, 1f, 0.45f));
        padBg.drawCircle(100, 100, 94);
        padBg.drawLine(100, 15, 100, 185);
        padBg.drawLine(15, 100, 185, 100);

        Pixmap padKnob = new Pixmap(76, 76, Pixmap.Format.RGBA8888);
        padKnob.setColor(new Color(0.85f, 0.85f, 0.85f, 0.8f));
        padKnob.fillCircle(38, 38, 34);
        padKnob.setColor(Color.WHITE);
        padKnob.drawCircle(38, 38, 34);

        TouchpadStyle touchpadStyle = new TouchpadStyle(
                new TextureRegionDrawable(registerTexture(padBg)),
                new TextureRegionDrawable(registerTexture(padKnob)));
        skin.add("default", touchpadStyle);

        skin.add("default", new LabelStyle(font, Color.WHITE));
        skin.add("title", new LabelStyle(font, Color.YELLOW));
    }

    private void createHudIcons() {
        // Crosshair dibuat lewat Pixmap (Tanda + dengan titik tengah)
        Pixmap ch = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        ch.setColor(Color.BLACK);
        ch.fillRectangle(14, 3, 4, 10);
        ch.fillRectangle(14, 19, 4, 10);
        ch.fillRectangle(3, 14, 10, 4);
        ch.fillRectangle(19, 14, 10, 4);
        ch.setColor(Color.WHITE);
        ch.fillRectangle(15, 4, 2, 8);
        ch.fillRectangle(15, 20, 2, 8);
        ch.fillRectangle(4, 15, 8, 2);
        ch.fillRectangle(20, 15, 8, 2);
        crosshairTex = registerTexture(ch);
    }

    @Override
    public void dispose() {
        skin.dispose();
        for (Texture tex : textures) {
            tex.dispose();
        }
    }
}