package no.elg.infiniteBootleg;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import no.elg.infiniteBootleg.server.Server;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @author Elg */
public class ServerMain extends CommonMain {

  private static ServerMain inst;

  @Nullable protected World serverWorld;

  @Nullable public Server server;

  public ServerMain(boolean test) {
    super(test);
    synchronized (INST_LOCK) {
      if (inst != null) {
        throw new IllegalStateException("A main instance have already be declared");
      }
      inst = this;
    }

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  if (serverWorld != null) {
                    serverWorld.save();
                    final FileHandle worldFolder = serverWorld.getWorldFolder();
                    if (worldFolder != null) {
                      worldFolder.deleteDirectory();
                    }
                  }
                  scheduler.shutdown(); // make sure scheduler threads are dead
                }));
  }

  @NotNull
  public static ServerMain inst() {
    return inst;
  }

  @NotNull
  public World getServerWorld() {
    if (serverWorld == null) {
      throw new IllegalStateException("There is no server world!");
    }
    return serverWorld;
  }

  public void setServerWorld(@Nullable World serverWorld) {
    synchronized (INST_LOCK) {
      this.serverWorld = serverWorld;
    }
  }

  @Override
  public void render() {
    //noinspection ConstantConditions Should be not null here
    serverWorld.getRender().render();
  }

  @Override
  public void create() {
    super.create();

    server = new Server();
    final Thread thread =
        new Thread(
            () -> {
              try {
                server.start();
              } catch (InterruptedException e) {
                console.log("SERVER", "Server interruption received", e);
                Gdx.app.exit();
              }
            },
            "Server");
    thread.setDaemon(true);
    thread.start();
    console.log("SERVER", "Starting server on port " + Settings.port);

    setServerWorld(new World(new PerlinChunkGenerator(Settings.worldSeed), Settings.worldSeed));
    getServerWorld().load();
  }

  @Override
  public void dispose() {
    super.dispose();
    if (serverWorld != null) {
      serverWorld.dispose();
    }
  }
}
