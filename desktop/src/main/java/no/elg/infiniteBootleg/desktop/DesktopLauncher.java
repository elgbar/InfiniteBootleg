package no.elg.infiniteBootleg.desktop;

import static no.elg.infiniteBootleg.ClientMain.SCALE;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
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

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        if (SCALE > 1) {
          config.setWindowedMode(1920, 1080);
        } else {
          config.setWindowedMode(1280, 720);
        }
        config.useVsync(false);
        config.setTitle("Infinite Terraria");
        config.useOpenGL3(false, 3, 2);
        config.setBackBufferConfig(8, 8, 8, 8, 0, 0, 8);
        new Lwjgl3Application(main, config);
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
