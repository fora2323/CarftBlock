package org.carft.block.game;

import com.badlogic.gdx.Game;
import org.carft.block.screen.HomeScreen;
import org.carft.block.ui.UiAssets;

public class CarftBlockGame extends Game {
    public UiAssets uiAssets;

    @Override
    public void create() {
        uiAssets = new UiAssets();
        setScreen(new HomeScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        if (uiAssets != null) {
            uiAssets.dispose();
        }
    }
}