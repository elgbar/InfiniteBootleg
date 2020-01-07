package no.elg.infiniteBootleg.console;

import box2dLight.DirectionalLight;
import com.badlogic.gdx.graphics.Color;
import com.strongjoshua.console.CommandExecutor;
import com.strongjoshua.console.LogLevel;
import com.strongjoshua.console.annotation.ConsoleDoc;
import com.strongjoshua.console.annotation.HiddenCommand;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.screen.HUDRenderer;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.enitites.GenericEntity;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
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
            Color skylight = Main.inst().getWorld().getBaseColor();
            try {
                Color color = (Color) Reflection.getStaticField(Color.class, colorName.toUpperCase());
                skylight.set(color);
                logger.log("Sky color changed to " + colorName.toLowerCase());
            } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
                logger.logf(LogLevel.ERROR, "Unknown color '%s'", colorName);
            }
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
        Main.inst().getWorld().unloadChunks(force, true);
        logger.log(LogLevel.SUCCESS, "All chunks have been reloaded");
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Toggle flight for player")
    public void fly() {
        Entity player = Main.inst().getWorld().getLivingEntities().iterator().next(); //assume this is the player
        player.setFlying(!player.isFlying());
        logger.log(LogLevel.SUCCESS, "Player is now " + (player.isFlying() ? "" : "not ") + "flying");
    }

    @ConsoleDoc(description =
                    "Pauses the world ticker. This includes Box2D world updates, light updates, unloading of chunks," +
                    " entity updates and chunks update")
    public void pause() {
        Main.inst().getWorld().getWorldTicker().pause();
        logger.log(LogLevel.SUCCESS, "World is now paused");
    }

    @ConsoleDoc(description =
                    "Resumes the world ticker. This includes Box2D world updates, light updates, unloading of chunks," +
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
        Main.debug = true;
        logger.log(LogLevel.SUCCESS,
                   "Debug rendering for Box2D is now " + (WorldRender.debugBox2d ? "enabled" : "disabled"));
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
        logger.logf(LogLevel.SUCCESS, "Teleported to (% d,% d)", worldX, worldY);
    }

    @ClientsideOnly
    @ConsoleDoc(description = "The quality of the light. To disable light use command 'light'",
                paramDescriptions = "Quality of light, between 0 and 4")
    public void lightQuality(int quality) {
        if (quality > 4) {
            logger.warn("Quality given is greater than the maximum. It has been set to 4");
            quality = 4;
        }
        else if (quality < 0) {
            logger.warn("Quality given is a negative number. It has been set to 0");
            quality = 0;
        }
        Main.inst().getWorld().getRender().getRayHandler().setBlurNum(quality);
        logger.success("Light quality is now " + quality);
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Set how much information to show", paramDescriptions = "normal (default), debug or none")
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
        if (zoom < WorldRender.MIN_ZOOM) {
            logger.warn("Given zoom level (%.3f) is less than the minimum % .3f", zoom, WorldRender.MIN_ZOOM);
            zoom = WorldRender.MIN_ZOOM;
        }
        WorldRender render = Main.inst().getWorld().getRender();
        render.getCamera().zoom = Math.max(zoom, WorldRender.MIN_ZOOM);
        render.update();
        logger.success("Zoom level is now " + zoom);
    }

    @HiddenCommand
    @ClientsideOnly
    public void paint() {
        Player player = Main.inst().getPlayer();
        if (player == null) {
            logger.error("PLR", "Failed to find any players");
            return;
        }
        for (Block block : player.touchingBlocks()) {
            block.setBlock(Material.TORCH);
        }
    }

    @ConsoleDoc(description = "Spawn a generic static entity at the given location with the given width and height",
                paramDescriptions = {"worldX", "worldY", "width", "height"})
    public void ent(float worldX, float worldY, int width, int height) {
        logger.logf("Created an entity at (% 7.2f,% 7.2f) with width %d and height %d", worldX, worldY, width, height);
        new GenericEntity(Main.inst().getWorld(), worldX, worldY, width, height);
    }

    @ConsoleDoc(description = "Kill all non-player entities")
    public void killall(boolean players) {
        World world = Main.inst().getWorld();
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Player)) {
                continue;
            }
            world.removeEntity(entity);
        }
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Set the brush size of the mouse", paramDescriptions = "New brush size, positive integer")
    public void brush(float size) {
        Player player = Main.inst().getPlayer();
        if (player == null) {
            logger.error("PLR", "Failed to find any players");
            return;
        }
        Main.inst().getPlayer().getControls().setBrushSize(size);
        logger.log(LogLevel.SUCCESS, "Brush size for player is now " + size);
    }

    @ConsoleDoc(description = "How fast the time flows", paramDescriptions = "The new scale of time")
    public void timescale(float scale) {
        float old = Main.inst().getWorld().getTimeScale();
        Main.inst().getWorld().setTimeScale(scale);
        logger.success("Changed time scale from % .3f to % .3f", old, scale);
    }

    @ConsoleDoc(description = "Toggle if time ticks or not")
    public void toggleTime() {
        World.dayTicking = !World.dayTicking;
        logger.success("Time is now " + (World.dayTicking ? "" : "not ") + "ticking");
    }

    @ConsoleDoc(description = "Set the current time", paramDescriptions = "The new time")
    public void time(float time) {
        float old = Main.inst().getWorld().getTime();
        Main.inst().getWorld().setTime(time);
        logger.success("Changed time from % .3f to % .3f", old, time);
    }

    @ConsoleDoc(description = "Set the current time", paramDescriptions = "Time of day")
    public void time(String timeOfDay) {
        float time;
        try {
            //There is a chance this method is before selected the other time method
            time = Float.parseFloat(timeOfDay);
        } catch (NumberFormatException ignored) {
            switch (timeOfDay.toLowerCase()) {
                case "day":
                case "dawn":
                case "sunrise":
                    time = World.SUNRISE_TIME;
                    break;
                case "midday":
                case "noon":
                    time = World.MIDDAY_TIME;
                    break;
                case "dusk":
                case "sunset":
                    time = World.SUNSET_TIME;
                    break;
                case "midnight":
                case "night":
                    time = World.MIDNIGHT_TIME;
                    break;
                case "end":
                    time = Integer.MAX_VALUE;
                    break;
                default:
                    logger.error("ERR", "Unknown time of day, try sunrise, midday, sunset or midnight");
                    return;
            }
        }

        //call the other time function
        time(time);
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Reset camera and zoom")
    public void reset() {
        Player player = Main.inst().getPlayer();
        if (player == null) {
            logger.error("PLR", "Failed to find any players");
            return;
        }
        WorldRender render = Main.inst().getWorld().getRender();
        render.getCamera().zoom = 1f;
        render.getCamera().position.x = player.getPosition().x * Block.BLOCK_SIZE;
        render.getCamera().position.y = player.getPosition().y * Block.BLOCK_SIZE;
        render.update();


    }
}

