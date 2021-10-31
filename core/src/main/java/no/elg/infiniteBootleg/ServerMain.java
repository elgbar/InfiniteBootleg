package no.elg.infiniteBootleg;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Collections;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.console.ConsoleHandler;
import no.elg.infiniteBootleg.console.ConsoleLogger;
import no.elg.infiniteBootleg.server.Server;
import no.elg.infiniteBootleg.util.CancellableThreadScheduler;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public class ServerMain implements Main {

    private static ServerMain inst;

    protected final boolean test;
    @NotNull
    protected final CancellableThreadScheduler scheduler;
    @Nullable
    protected World world;
    @NotNull
    protected ConsoleHandler console;

    @Nullable
    public Server server;

    static {
        //use unique iterators
        Collections.allocateIterators = true;
    }

    public ServerMain(boolean test) {
        synchronized (INST_LOCK) {
            if (inst != null) {
                throw new IllegalStateException("A main instance have already be declared");
            }
            inst = this;
        }
        this.test = test;
        scheduler = new CancellableThreadScheduler(Settings.schedulerThreads);
        console = new ConsoleHandler(false);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (world != null) {
                world.save();
                final FileHandle worldFolder = world.getWorldFolder();
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

    @Override
    public @NotNull ConsoleLogger getConsoleLogger() {
        return console;
    }

    @Override
    @NotNull
    public World getWorld() {
        if (world == null) {
            throw new IllegalStateException("There is no world when not in world screen");
        }
        return world;
    }

    @Override
    public void setWorld(@Nullable World world) {
        synchronized (INST_LOCK) {
            this.world = world;
        }
    }

    @Override
    public @NotNull ConsoleHandler getConsole() {
        return console;
    }

    @Override
    public @NotNull CancellableThreadScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public boolean isNotTest() {
        return !test;
    }

    @Override
    public void create() {
        Gdx.app.setApplicationLogger(console);
        Gdx.app.setLogLevel(test || Settings.debug ? Application.LOG_DEBUG : Application.LOG_INFO);

        console = new ConsoleHandler(Settings.client);
        console.setAlpha(0.85f);
        console.log(LogLevel.SUCCESS, "Version #" + Util.getVersion());
        console.log("You can also start the program with arguments for '--help' or '-?' as arg to see all possible options");

        if (Settings.isServer()) {
            server = new Server();
            final Thread thread = new Thread(() -> {
                try {
                    server.start();
                } catch (InterruptedException e) {
                    console.log("SERVER", "Server interruption received", e);
                    Gdx.app.exit();
                }
            }, "Server");
            thread.setDaemon(true);
            thread.start();
            console.log("SERVER", "Starting server on port " + Settings.port);

            setWorld(new World(new PerlinChunkGenerator(Settings.worldSeed)));
            getWorld().load();
        }
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void render() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {
        console.dispose();
        if (world != null) {
            world.dispose();
        }
    }
}
