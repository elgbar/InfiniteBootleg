package no.elg.infiniteBootleg.console;

import box2dLight.DirectionalLight;
import box2dLight.RayHandler;
import com.strongjoshua.console.CommandExecutor;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Chunk;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
@SuppressWarnings("unused")
public class Commands extends CommandExecutor {

    private ConsoleLogger logger;

    public Commands(@NotNull ConsoleLogger logger) {
        this.logger = logger;
    }

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

    public void setShadows(boolean status) {
        RayHandler rayHandler = Main.inst().getWorld().getRender().getRayHandler();
        rayHandler.setShadows(status);
        logger.log("Shadows are now " + status);
    }

    public void reloadTextures() {
        for (Chunk chunk : Main.inst().getWorld().getLoadedChunks()) {
            chunk.updateTextureNow();
        }

    }

}

