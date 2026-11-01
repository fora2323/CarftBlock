package org.carft.block;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.WindowManager;

public class MainActivity extends Activity {

    GUI.MenuView menu;
    public Game game;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefs = getSharedPreferences("GoBlockSettings", MODE_PRIVATE);
        Game.sensIndex = prefs.getInt("sensIndex", 1);
        Game.lookSens = CraftEngine.SENS_VALUES[Game.sensIndex];
        Game.renderDist = prefs.getInt("renderDist", 20);

        menu = new GUI.MenuView(this);
        setContentView(menu);
    }

    public void startGame() {
        game = new Game(this);
        setContentView(game);
    }

    public void backToMenu() {
        game = null;
        menu.screen = 0;
        if (!menu.ticking) {
            menu.ticking = true;
            menu.postDelayed(menu.ticker, 33);
        }
        setContentView(menu);
    }

    public void saveSettings() {
        prefs.edit()
             .putInt("sensIndex", Game.sensIndex)
             .putInt("renderDist", Game.renderDist)
             .apply();
    }

    @Override
    public void onBackPressed() {
        if (game != null) backToMenu();
        else super.onBackPressed();
    }
}