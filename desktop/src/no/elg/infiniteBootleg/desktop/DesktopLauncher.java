package no.elg.infiniteBootleg.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import no.elg.infiniteBootleg.Main;

public class DesktopLauncher {

    public static void main(String[] args) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.width = 1280;
        config.height = 720;
        config.vSyncEnabled = true;
        config.foregroundFPS = 9999;
        config.backgroundFPS = 10;
        config.samples = 16;
        config.title = "Infinite Terraria";
        new LwjglApplication(new Main(args), config);
    }
}
