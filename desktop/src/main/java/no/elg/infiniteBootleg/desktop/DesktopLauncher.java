package no.elg.infiniteBootleg.desktop;

import static no.elg.infiniteBootleg.ClientMain.SCALE;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.ServerMain;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.args.ProgramArgs;
import no.elg.infiniteBootleg.util.Ticker;

public class DesktopLauncher {

  public static void main(String[] args) {

    new ProgramArgs(args);
    try {
      if (Settings.client) {
        Main main = new ClientMain(false);

        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        if (SCALE > 1) {
          config.width = 1920;
          config.height = 1080;
        } else {
          config.width = 1280;
          config.height = 720;
        }
        config.vSyncEnabled = false;
        config.foregroundFPS = 9999;
        config.backgroundFPS = 60;
        config.samples = 16;
        config.title = "Infinite Terraria";
        config.useHDPI = true;
        new LwjglApplication(main, config);
      } else {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond =
            (int) (Settings.tps < 0 ? Ticker.DEFAULT_TICKS_PER_SECOND : Settings.tps);
        Main main = new ServerMain(false);
        new HeadlessApplication(main, config);
      }
    } catch (Throwable t) {
      System.err.println("Uncaught exception thrown: " + t.getClass().getSimpleName());
      t.printStackTrace();
    }
  }
}
