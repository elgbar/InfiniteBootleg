package no.elg.infiniteBootleg.console;

import box2dLight.DirectionalLight;
import box2dLight.RayHandler;
import com.strongjoshua.console.CommandExecutor;
import com.strongjoshua.console.LogLevel;
import com.strongjoshua.console.annotation.ConsoleDoc;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import org.jetbrains.annotations.NotNull;

import static no.elg.infiniteBootleg.world.render.WorldRender.debugBox2d;

/**
 * @author Elg
 */
public class Commands extends CommandExecutor {

    private ConsoleLogger logger;

    public Commands(@NotNull ConsoleLogger logger) {
        this.logger = logger;
    }

    @ConsoleDoc(description = "Set the color of the sky! Params are expected to be between 0 and 1",
                paramDescriptions = {"red", "green", "blue", "alpha"})
    public void setSkyColor(float r, float g, float b, float a) {
        if (Main.renderGraphic) {
            DirectionalLight skylight = Main.inst().getWorld().getRender().getSkylight();
            skylight.setColor(r, g, b, a);
            logger.log("Sky color changed to " + skylight.getColor());
        }
        else {
            logger.log(LogLevel.ERROR, "Cannot change the color of the sky as graphics are not enabled");
        }
    }

    @ConsoleDoc(description = "Disable/enable lights", paramDescriptions = "status")
    public void lights(boolean status) {
        RayHandler rayHandler = Main.inst().getWorld().getRender().getRayHandler();
        rayHandler.setShadows(status);
        logger.log(LogLevel.SUCCESS, "Lighting is now " + (status ? "enabled" : "disabled"));
    }

    @ConsoleDoc(description = "Reloads chunks internal state, its texture and Box2D body ")
    public void reload() {
        for (Chunk chunk : Main.inst().getWorld().getLoadedChunks()) {
            chunk.updateTextureNow();
        }
        logger.log(LogLevel.SUCCESS, "All textures of loaded chunks updated");
    }

    @ConsoleDoc(description = "Toggle flight for player")
    public void fly() {
        Entity player = Main.inst().getWorld().getEntities().iterator().next(); //assume this is the player
        player.setFlying(!player.isFlying());
        logger.log(LogLevel.SUCCESS, "Player is now " + (player.isFlying() ? "" : "not") + " flying");
    }

    @ConsoleDoc(description = "Pauses the world ticker. This includes Box2D world updates, light updates, unloading of chunks," +
                              " entity updates and chunks update")
    public void pause() {
        Main.inst().getWorld().getWorldTicker().pause();
        logger.log(LogLevel.SUCCESS, "World is now paused");
    }

    @ConsoleDoc(description = "Resumes the world ticker. This includes Box2D world updates, light updates, unloading of chunks," +
                              " entity updates and chunks update")
    public void resume() {
        Main.inst().getWorld().getWorldTicker().resume();
        logger.log(LogLevel.SUCCESS, "World is now resumed");
    }

    @ConsoleDoc(description = "Toggles debug rendering of Box2D objects")
    public void debugBox2d() {
        debugBox2d = !debugBox2d;
        logger.log(LogLevel.SUCCESS, "Debug rendering for Box2D is now " + (debugBox2d ? "enabled" : "disabled"));
    }

}

