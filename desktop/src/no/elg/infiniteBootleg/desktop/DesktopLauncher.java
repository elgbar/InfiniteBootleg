package no.elg.infiniteBootleg.desktop;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import no.elg.infiniteBootleg.Main;

import static no.elg.infiniteBootleg.args.ProgramArgs.executeArgs;

public class DesktopLauncher {

    public static void main(String[] args) {

        executeArgs(args);

        if (Main.renderGraphic) {

            LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
            config.width = 1280;
            config.height = 720;
            config.vSyncEnabled = false;
            config.foregroundFPS = 9999;
            config.backgroundFPS = 10;
            config.samples = 16;
            config.title = "Infinite Terraria";
            config.useHDPI = true;
            new LwjglApplication(new Main(), config);
        }
        else {
            new HeadlessApplication(new Main(), null);
        }
    }
}
