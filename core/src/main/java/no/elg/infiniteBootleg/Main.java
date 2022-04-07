package no.elg.infiniteBootleg;

import com.badlogic.gdx.ApplicationListener;
import java.io.File;
import no.elg.infiniteBootleg.console.ConsoleHandler;
import no.elg.infiniteBootleg.console.ConsoleLogger;
import no.elg.infiniteBootleg.util.CancellableThreadScheduler;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public interface Main extends ApplicationListener {

  String EXTERNAL_FOLDER = ".infiniteBootleg" + File.separatorChar;
  String WORLD_FOLDER = EXTERNAL_FOLDER + "worlds" + File.separatorChar;
  String TEXTURES_FOLDER = "textures" + File.separatorChar;
  String FONTS_FOLDER = "fonts" + File.separatorChar;
  String TEXTURES_BLOCK_FILE = TEXTURES_FOLDER + "blocks.atlas";
  String TEXTURES_ENTITY_FILE = TEXTURES_FOLDER + "entities.atlas";
  String VERSION_FILE = "version";

  Object INST_LOCK = new Object();

  static @NotNull ConsoleLogger logger() {
    return inst().getConsoleLogger();
  }

  @NotNull
  static Main inst() {
    return CommonMain.inst();
  }

  /**
   * @return If this is a client of a server
   */
  static boolean isServerClient() {
    return Settings.client && ClientMain.inst().getServerClient() != null;
  }

  /**
   * @return If this is a server instance (i.e., is NOT rendering)
   */
  static boolean isServer() {
    return !Settings.client;
  }

  /**
   * @return If this is a client instance (i.e., is rendering)
   */
  static boolean isClient() {
    return Settings.client;
  }

  /**
   * @return If this is a singleplayer instance
   */
  static boolean isSingleplayer() {
    return Settings.client && ClientMain.inst().isSinglePlayer();
  }

  /**
   * @return If the current instance is multiplayer (either as the server or a client of a server)
   */
  static boolean isMultiplayer() {
    return !Settings.client || ClientMain.inst().isMultiplayer();
  }

  /**
   * @return If this instance is authoritative (i.e., have the final say)
   */
  static boolean isAuthoritative() {
    return isServer() || isSingleplayer();
  }

  @NotNull
  ConsoleLogger getConsoleLogger();

  @NotNull
  ConsoleHandler getConsole();

  @NotNull
  CancellableThreadScheduler getScheduler();

  boolean isNotTest();

  /**
   * Might not return any world if the player is in a menu.
   *
   * @return The current world
   */
  @Nullable
  World getWorld();
}
