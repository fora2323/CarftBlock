package org.carft.block.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class UiAssets implements Disposable {
    public Skin skin;
    private Array<Texture> textures; // Menyimpan referensi tekstur agar bisa di-dispose

    public UiAssets() {
        skin = new Skin();
        textures = new Array<>();

        // 1. Setup Font Default
        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.5f); // Perbesar font bawaan agar tidak terlalu kecil
        skin.add("default-font", font);

        // 2. Buat tekstur polos (Pixel Putih)
        Pixmap pixWhite = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixWhite.setColor(Color.WHITE);
        pixWhite.fill();
        Texture texWhite = new Texture(pixWhite);
        textures.add(texWhite);
        pixWhite.dispose();
        skin.add("white", new TextureRegionDrawable(texWhite));

        // 3. Buat tekstur tombol biasa (Abu-abu terang dengan border gelap)
        Pixmap pixBtn = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pixBtn.setColor(Color.DARK_GRAY);
        pixBtn.fill();
        pixBtn.setColor(Color.LIGHT_GRAY);
        pixBtn.fillRectangle(2, 2, 60, 60);
        Texture texBtn = new Texture(pixBtn);
        textures.add(texBtn);
        pixBtn.dispose();

        // 4. Buat tekstur tombol ditekan (Abu-abu gelap)
        Pixmap pixBtnDown = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pixBtnDown.setColor(Color.BLACK);
        pixBtnDown.fill();
        pixBtnDown.setColor(Color.GRAY);
        pixBtnDown.fillRectangle(4, 4, 56, 56);
        Texture texBtnDown = new Texture(pixBtnDown);
        textures.add(texBtnDown);
        pixBtnDown.dispose();

        // 5. Daftarkan style tombol
        TextButtonStyle btnStyle = new TextButtonStyle();
        btnStyle.up = new TextureRegionDrawable(texBtn);
        btnStyle.down = new TextureRegionDrawable(texBtnDown);
        btnStyle.font = font;
        btnStyle.fontColor = Color.WHITE;
        skin.add("default", btnStyle);

        // 6. Buat tekstur untuk Slider (Sensitivitas)
        Pixmap pixSliderBg = new Pixmap(100, 10, Pixmap.Format.RGBA8888);
        pixSliderBg.setColor(Color.DARK_GRAY);
        pixSliderBg.fill();
        Texture texSliderBg = new Texture(pixSliderBg);
        textures.add(texSliderBg);
        pixSliderBg.dispose();

        Pixmap pixSliderKnob = new Pixmap(20, 30, Pixmap.Format.RGBA8888);
        pixSliderKnob.setColor(Color.LIGHT_GRAY);
        pixSliderKnob.fill();
        Texture texSliderKnob = new Texture(pixSliderKnob);
        textures.add(texSliderKnob);
        pixSliderKnob.dispose();

        SliderStyle sliderStyle = new SliderStyle();
        sliderStyle.background = new TextureRegionDrawable(texSliderBg);
        sliderStyle.knob = new TextureRegionDrawable(texSliderKnob);
        skin.add("default-horizontal", sliderStyle);
    }

    @Override
    public void dispose() {
        skin.dispose();
        for (Texture tex : textures) {
            tex.dispose();
        }
    }
}