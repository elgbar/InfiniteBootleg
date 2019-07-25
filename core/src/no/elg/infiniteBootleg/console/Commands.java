package no.elg.infiniteBootleg.console;

import box2dLight.DirectionalLight;
import com.badlogic.gdx.graphics.Color;
import com.strongjoshua.console.CommandExecutor;
import com.strongjoshua.console.LogLevel;
import com.strongjoshua.console.annotation.ConsoleDoc;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.HUDRenderer;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.kh498.util.Reflection;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class Commands extends CommandExecutor {

    private ConsoleLogger logger;

    public Commands(@NotNull ConsoleLogger logger) {
        this.logger = logger;
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Set the color of the sky. Params are expected to be between 0 and 1",
                paramDescriptions = {"red", "green", "blue", "alpha"})
    public void skyColor(float r, float g, float b, float a) {
        DirectionalLight skylight = Main.inst().getWorld().getRender().getSkylight();
        skylight.setColor(r, g, b, a);
        logger.log("Sky color changed to " + skylight.getColor());

    }

    @ClientsideOnly
    @ConsoleDoc(description = "Set the color of the sky", paramDescriptions = {"Name of color"})
    public void skyColor(String colorName) {
        if (Main.renderGraphic) {
            DirectionalLight skylight = Main.inst().getWorld().getRender().getSkylight();
            try {
                Color color = (Color) Reflection.getStaticField(Color.class, colorName.toUpperCase());
                skylight.setColor(color);
            } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
                logger.logf(LogLevel.ERROR, "Unknown color '%s'", colorName);
                return;
            }
            logger.log("Sky color changed to " + colorName.toLowerCase());
        }
        else {
            logger.log(LogLevel.ERROR, "Cannot change the color of the sky as graphics are not enabled");
        }
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Toggle rendering of lights")
    public void lights() {
        WorldRender.lights = !WorldRender.lights;
        if (WorldRender.lights) { Main.inst().getWorld().getRender().update(); }
        logger.log(LogLevel.SUCCESS, "Lighting is now " + (WorldRender.lights ? "enabled" : "disabled"));
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Reload all loaded chunks if unloading is allowed")
    public void reload() {
        reload(false);
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Reload all loaded chunks",
                paramDescriptions = "Force unloading of chunks even when unloading is disallowed")
    public void reload(boolean force) {
        World world = Main.inst().getWorld();
        for (Chunk chunk : world.getLoadedChunks()) {
            if (force) { chunk.setAllowUnload(true); }
            world.unload(chunk);
        }
        logger.log(LogLevel.SUCCESS, "All chunks reloaded");
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Toggle flight for player")
    public void fly() {
        Entity player = Main.inst().getWorld().getPlayers().iterator().next(); //assume this is the player
        player.setFlying(!player.isFlying());
        logger.log(LogLevel.SUCCESS, "Player is now " + (player.isFlying() ? "" : "not ") + "flying");
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
        World world = Main.inst().getWorld();
        world.getWorldTicker().resume();
        world.getRender().update();
        logger.log(LogLevel.SUCCESS, "World is now resumed");
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Toggles debug rendering of Box2D objects")
    public void debug() {
        WorldRender.debugBox2d = !WorldRender.debugBox2d;
        logger.log(LogLevel.SUCCESS, "Debug rendering for Box2D is now " + (WorldRender.debugBox2d ? "enabled" : "disabled"));
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Teleport to given world coordinate",
                paramDescriptions = {"World x coordinate", "World y coordinate"})
    public void tp(int worldX, int worldY) {
        if (!Main.renderGraphic) {
            return;
        }
        WorldRender render = Main.inst().getWorld().getRender();
        render.getCamera().position.x = worldX * Block.BLOCK_SIZE;
        render.getCamera().position.y = worldY * Block.BLOCK_SIZE;
        render.update();
    }

    @ClientsideOnly
    @ConsoleDoc(description = "The quality of the light. To disable light use command 'light'",
                paramDescriptions = "Quality of light, between 0 and 4")
    public void lightQuality(int quality) {
        if (quality > 4) { quality = 4; }
        else if (quality < 0) { quality = 0; }
        Main.inst().getWorld().getRender().getRayHandler().setBlurNum(quality);
    }

    @ClientsideOnly
    @ConsoleDoc(description = "The direction of the skylight", paramDescriptions = "A float between 0 and 180")
    public void lightDir(float dir) {
        if (dir < 0) {
            logger.log(LogLevel.ERROR, "Direction can not be less than 0");
            return;
        }
        else if (dir > 180) {
            logger.log(LogLevel.ERROR, "Direction can not be greater than or equal to 180");
            return;
        }
        Main.inst().getWorld().getRender().getSkylight().setDirection(-dir);
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Set how much information to give", paramDescriptions = "normal (default), minimal or none")
    public void hud(String modusName) {
        try {
            HUDRenderer.HUDModus modus = HUDRenderer.HUDModus.valueOf(modusName.toUpperCase());
            Main.inst().getHud().setModus(modus);
        } catch (IllegalArgumentException e) {
            logger.log(LogLevel.ERROR, "Unknown HUD modus '" + modusName + "'");
        }
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Change the zoom level of the world camera",
                paramDescriptions = "The new zoom level, min is " + WorldRender.MIN_ZOOM)
    public void zoom(float zoom) {
        Main.inst().getWorld().getRender().getCamera().zoom = Math.max(zoom, WorldRender.MIN_ZOOM);
        Main.inst().getWorld().getRender().update();
    }

    @ClientsideOnly
    public void paint() {
        Player player = Main.inst().getWorld().getPlayers().iterator().next();
        //noinspection LibGDXUnsafeIterator
        for (Block block : player.touchingBlocks()) {
            block.setBlock(Material.TORCH);
            System.out.println("block.toString() = " + block.toString());
        }
    }

    public void ent(float worldX, float worldY, int width, int height) {
        logger.logf("Created an entity at (% 7.2f,% 7.2f) with width %d and height %d", worldX, worldY, width, height);
        new GenericEntity(Main.inst().getWorld(), worldX, worldY, width, height);
    }
}

