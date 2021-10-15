package no.elg.infiniteBootleg.desktop;

import static no.elg.infiniteBootleg.Main.SCALE;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.args.ProgramArgs;

public class DesktopLauncher {

    public static void main(String[] args) {

        Main main = new Main(false);

        new ProgramArgs(args);

        try {
            if (Settings.client) {

                LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
                if (SCALE > 1) {
                    config.width = 1920;
                    config.height = 1080;
                }
                else {
                    config.width = 1280;
                    config.height = 720;
                }
                config.vSyncEnabled = false;
                config.foregroundFPS = 9999;
                config.backgroundFPS = 10;
                config.samples = 16;
                config.title = "Infinite Terraria";
                config.useHDPI = true;
                new LwjglApplication(main, config);
            }
            else {
                new HeadlessApplication(main, null);
            }
        } catch (Throwable t) {
            System.out.println("Uncaught exception thrown: " + t.getClass().getSimpleName());
            t.printStackTrace();
        }
    }
}
