package no.elg.infiniteBootleg;

import com.badlogic.gdx.Gdx;
import no.elg.infiniteBootleg.args.ProgramArgs;
import no.elg.infiniteBootleg.server.PacketExtraKt;
import no.elg.infiniteBootleg.server.Server;
import no.elg.infiniteBootleg.world.ServerWorld;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public class ServerMain extends CommonMain {

  private static ServerMain inst;

  @Nullable protected ServerWorld serverWorld;

  @Nullable public Server server;
  private String renderThreadName;

  public ServerMain(boolean test, @Nullable ProgramArgs progArgs) {
    super(test, progArgs);
    synchronized (INST_LOCK) {
      if (inst != null) {
        throw new IllegalStateException("A main instance have already be declared");
      }
      inst = this;
    }

    Runnable onShutdown =
        () -> {
          if (server != null) {
            PacketExtraKt.broadcast(
                PacketExtraKt.clientBoundDisconnectPlayerPacket("Server closed"), null);
          }
          if (serverWorld != null) {
            serverWorld.save();
            serverWorld.dispose();
          }
          dispose();
          scheduler.shutdown(); // make sure scheduler threads are dead
        };
    Runtime.getRuntime().addShutdownHook(new Thread(onShutdown));
  }

  @NotNull
  public static ServerMain inst() {
    return inst;
  }

  @NotNull
  public ServerWorld getServerWorld() {
    if (serverWorld == null) {
      throw new IllegalStateException("There is no server world!");
    }
    return serverWorld;
  }

  public void setServerWorld(@Nullable ServerWorld serverWorld) {
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
    renderThreadName = Thread.currentThread().getName();

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

    // TODO load world name from some config
    setServerWorld(
        new ServerWorld(
            new PerlinChunkGenerator(Settings.worldSeed), Settings.worldSeed, "Server World"));
    getServerWorld().initialize();
  }

  @Override
  public World getWorld() {
    return serverWorld;
  }

  @Override
  public void dispose() {
    super.dispose();
    if (serverWorld != null) {
      serverWorld.dispose();
    }
  }

  @Override
  public @NotNull String getRenderThreadName() {
    return renderThreadName;
  }
}
