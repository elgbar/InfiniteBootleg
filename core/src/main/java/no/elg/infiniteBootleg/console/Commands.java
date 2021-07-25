package no.elg.infiniteBootleg.console;

import com.badlogic.gdx.graphics.Color;
import com.strongjoshua.console.CommandExecutor;
import com.strongjoshua.console.LogLevel;
import com.strongjoshua.console.annotation.ConsoleDoc;
import com.strongjoshua.console.annotation.HiddenCommand;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.input.EntityControls;
import no.elg.infiniteBootleg.screen.HUDRenderer;
import no.elg.infiniteBootleg.util.Ticker;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import static no.elg.infiniteBootleg.world.render.WorldRender.BOX2D_LOCK;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.enitites.GenericEntity;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import no.kh498.util.Reflection;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class Commands extends CommandExecutor {

    private final ConsoleLogger logger;

    public Commands(@NotNull ConsoleLogger logger) {
        this.logger = logger;
    }

    @CmdArgNames({"red", "green", "blue", "alpha"})
    @ClientsideOnly
    @ConsoleDoc(description = "Set the color of the sky. Params are expected to be between 0 and 1", paramDescriptions = {"red", "green", "blue", "alpha"})
    public void skyColor(float r, float g, float b, float a) {
        Color skylight = Main.inst().getWorld().getBaseColor();
        skylight.set(r, g, b, a);
        logger.success("Sky color changed to " + skylight);
    }


    @CmdArgNames("color")
    @ClientsideOnly
    @ConsoleDoc(description = "Set the color of the sky", paramDescriptions = {"Name of color"})
    public void skyColor(String colorName) {
        if (Settings.renderGraphic) {
            Color skylight = Main.inst().getWorld().getBaseColor();
            try {
                Color color = (Color) Reflection.getStaticField(Color.class, colorName.toUpperCase());
                skylight.set(color);
                logger.log("Sky color changed to " + colorName.toLowerCase() + " (" + color + ")");
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
        Settings.renderLight = !Settings.renderLight;
        if (Settings.renderLight) {
            Main.inst().getWorld().getRender().update();
        }
        logger.log(LogLevel.SUCCESS, "Lighting is now " + (Settings.renderLight ? "enabled" : "disabled"));
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Reload all loaded chunks if unloading is allowed")
    public void reload() {
        reload(false);
    }


    @CmdArgNames("force")
    @ClientsideOnly
    @ConsoleDoc(description = "Reload all loaded chunks", paramDescriptions = "Force unloading of chunks even when unloading is disallowed")
    public void reload(boolean force) {
        Main.inst().getWorld().unloadChunks(force, true);
        logger.log(LogLevel.SUCCESS, "All chunks have been reloaded");
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Toggle flight for player")
    public void fly() {
        Player player = Main.inst().getPlayer();
        if (player == null) {
            logger.error("CMD", "Failed to find any players");
            return;
        }
        player.setFlying(!player.isFlying());
        logger.log(LogLevel.SUCCESS, "Player is now " + (player.isFlying() ? "" : "not ") + "flying");
    }

    @ConsoleDoc(description = "Pauses the world ticker. This includes Box2D world updates, light updates, unloading of chunks," +
                              " entity updates and chunks update")
    public void pause() {
        Ticker ticker = Main.inst().getWorld().getWorldTicker();
        if (ticker.isPaused()) {
            logger.log(LogLevel.ERROR, "World is already paused");
        }
        else {
            ticker.pause();
            logger.log(LogLevel.SUCCESS, "World is now paused");
        }
    }

    @ConsoleDoc(description = "Resumes the world ticker. This includes Box2D world updates, light updates, unloading of chunks," +
                              " entity updates and chunks update")
    public void resume() {
        World world = Main.inst().getWorld();
        Ticker ticker = Main.inst().getWorld().getWorldTicker();
        if (ticker.isPaused()) {
            world.getWorldTicker().resume();
            world.getRender().update();
            logger.log(LogLevel.SUCCESS, "World is now resumed");
        }
        else {
            logger.log(LogLevel.ERROR, "World is not paused");
        }
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Toggles debug rendering of Box2D objects")
    public void debug() {
        Settings.renderBox2dDebug = !Settings.renderBox2dDebug;
        Settings.debug = true;
        logger.log(LogLevel.SUCCESS, "Debug rendering for Box2D is now " + (Settings.renderBox2dDebug ? "enabled" : "disabled"));
    }


    @ClientsideOnly
    @ConsoleDoc(description = "Toggles smoothed camera movement when following a player")
    public void lerp() {
        Settings.enableCameraFollowLerp = !Settings.enableCameraFollowLerp;
        logger.log(LogLevel.SUCCESS, "Camera lerp is now " + (Settings.enableCameraFollowLerp ? "enabled" : "disabled"));
    }


    @CmdArgNames({"x", "y"})
    @ClientsideOnly
    @ConsoleDoc(description = "Teleport to given world coordinate", paramDescriptions = {"World x coordinate", "World y coordinate"})
    public void tp(float worldX, float worldY) {
        if (!Settings.renderGraphic) {
            return;
        }
        WorldRender render = Main.inst().getWorld().getRender();
        render.getCamera().position.x = worldX * Block.BLOCK_SIZE;
        render.getCamera().position.y = worldY * Block.BLOCK_SIZE;
        render.update();
        logger.logf(LogLevel.SUCCESS, "Teleported camera to (% .2f,% .2f)", worldX, worldY);

        Player player = Main.inst().getPlayer();
        if (player == null) {
            logger.error("CMD", "Failed to find any players");
            return;
        }
        player.teleport(worldX, worldY, false);
        logger.logf(LogLevel.SUCCESS, "Teleported player to (% .2f,% .2f)", worldX, worldY);
    }

    @CmdArgNames({"quality"})
    @ClientsideOnly
    @ConsoleDoc(description = "The quality of the light. To disable light use command 'light'", paramDescriptions = "Quality of light, between 0 and 4")
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

    @CmdArgNames({"modus"})
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

    @CmdArgNames({"zoom level"})
    @ClientsideOnly
    @ConsoleDoc(description = "Change the zoom level of the world camera", paramDescriptions = "The new zoom level, min is " + WorldRender.MIN_ZOOM)
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
            logger.error("CMD", "Failed to find any players");
            return;
        }
        for (Block block : player.touchingBlocks()) {
            block.setBlock(Material.TORCH);
        }
    }

    @CmdArgNames({"x", "y", "width", "height"})
    @ConsoleDoc(description = "Spawn a generic static entity at the given location with the given width and height",
                paramDescriptions = {"worldX", "worldY", "width", "height"})
    public void ent(float worldX, float worldY, int width, int height) {
        logger.logf("Created an entity at (% 7.2f,% 7.2f) with width %d and height %d", worldX, worldY, width, height);
        new GenericEntity(Main.inst().getWorld(), worldX, worldY, width, height);
    }

    @ConsoleDoc(description = "Kill all non-player entities")
    public void killall() {
        World world = Main.inst().getWorld();
        int entities;
        synchronized (BOX2D_LOCK) {
            entities = world.getEntities().size();
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Player) {
                    continue;
                }
                world.removeEntity(entity);
            }
        }
        logger.log(LogLevel.SUCCESS, "Killed " + entities + " entities");
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Get the brush sizes")
    public void brush() {
        Player player = Main.inst().getPlayer();
        if (player == null) {
            logger.error("CMD", "Failed to find any players");
            return;
        }
        EntityControls controls = Main.inst().getPlayer().getControls();
        logger.logf(LogLevel.SUCCESS, "Brush size for player are now %.2f blocks for breaking and %.2f blocks for placing", controls.getBreakBrushSize(),
                    controls.getPlaceBrushSize());
    }

    @CmdArgNames({"type", "size"})
    @ClientsideOnly
    @ConsoleDoc(description = "Set the brush size of the mouse",
                paramDescriptions = {"Type of brush to change, can be 'break' and 'place'", "New brush size, positive integer"})
    public void brush(String type, float size) {
        Player player = Main.inst().getPlayer();
        if (player == null) {
            logger.error("CMD", "Failed to find any players");
            return;
        }
        EntityControls controls = Main.inst().getPlayer().getControls();

        if ("break".equalsIgnoreCase(type)) {
            controls.setBreakBrushSize(size);
        }
        else if ("place".equalsIgnoreCase(type)) {
            controls.setPlaceBrushSize(size);
        }
        else {
            logger.error("CMD", "Valid brush types are 'break' and 'place'");
            return;
        }
        brush();
    }

    @CmdArgNames({"scale"})
    @ConsoleDoc(description = "How fast the time flows", paramDescriptions = "The new scale of time")
    public void timescale(float scale) {
        float old = Main.inst().getWorld().getTimeScale();
        Main.inst().getWorld().setTimeScale(scale);
        logger.success("Changed time scale from % .3f to % .3f", old, scale);
    }

    @ConsoleDoc(description = "Toggle if time ticks or not")
    public void toggleTime() {
        Settings.dayTicking = !Settings.dayTicking;
        logger.success("Time is now " + (Settings.dayTicking ? "" : "not ") + "ticking");
    }


    @CmdArgNames({"time of day"})
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
                    logger.error("CMD", "Unknown time of day, try sunrise, midday, sunset or midnight");
                    return;
            }
        }

        //call the other time function
        time(time);
    }

    @CmdArgNames({"time"})
    @ConsoleDoc(description = "Set the current time", paramDescriptions = "The new time")
    public void time(float time) {
        float old = Main.inst().getWorld().getTime();
        Main.inst().getWorld().setTime(time);
        logger.success("Changed time from % .3f to % .3f", old, time);
    }

    @ClientsideOnly
    @ConsoleDoc(description = "Reset camera and zoom")
    public void reset() {
        Player player = Main.inst().getPlayer();
        if (player == null) {
            logger.error("CMD", "Failed to find any players");
            return;
        }
        World world = Main.inst().getWorld();
        WorldRender render = world.getRender();
        render.getCamera().zoom = 1f;
        render.getCamera().position.x = player.getPosition().x * Block.BLOCK_SIZE;
        render.getCamera().position.y = player.getPosition().y * Block.BLOCK_SIZE;
        if (world.getInput() != null) {
            world.getInput().setLockedOn(true);
        }
        render.update();
    }
}

