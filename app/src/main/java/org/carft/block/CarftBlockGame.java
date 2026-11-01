package org.carft.block.game;

import com.badlogic.gdx.Game;
import org.carft.block.ui.UiAssets;
import org.carft.block.screen.HomeScreen;

public class CarftBlockGame extends Game {
    public UiAssets uiAssets;

    @Override
    public void create() {
        // Inisialisasi semua aset UI hasil generate Pixmap sekali saat awal
        uiAssets = new UiAssets();
        
        // Pindah ke layar utama (Home)
        this.setScreen(new HomeScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        // Bersihkan memori aset UI saat game ditutup sepenuhnya
        if (uiAssets != null) {
            uiAssets.dispose();
        }
    }
}