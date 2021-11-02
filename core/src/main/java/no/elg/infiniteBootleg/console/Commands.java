package no.elg.infiniteBootleg.console;

import static no.elg.infiniteBootleg.world.render.WorldRender.BOX2D_LOCK;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;
import com.strongjoshua.console.CommandExecutor;
import com.strongjoshua.console.LogLevel;
import com.strongjoshua.console.annotation.ConsoleDoc;
import com.strongjoshua.console.annotation.HiddenCommand;
import java.util.UUID;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.ServerMain;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.input.EntityControls;
import no.elg.infiniteBootleg.screen.HUDRenderer;
import no.elg.infiniteBootleg.screens.ConnectingScreen;
import no.elg.infiniteBootleg.screens.MainMenuScreen;
import no.elg.infiniteBootleg.screens.WorldScreen;
import no.elg.infiniteBootleg.server.PacketExtraKt;
import no.elg.infiniteBootleg.server.ServerClient;
import no.elg.infiniteBootleg.util.Ticker;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.enitites.GenericEntity;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import no.elg.infiniteBootleg.world.time.WorldTime;
import no.kh498.util.Reflection;
import org.jetbrains.annotations.NotNull;

/** @author Elg */
public class Commands extends CommandExecutor {

  private final ConsoleLogger logger;

  public Commands(@NotNull ConsoleLogger logger) {
    this.logger = logger;
  }

  @NotNull
  public World getWorld() {
    if (Settings.client) {
      return ClientMain.inst().getWorld();
    } else {
      return ServerMain.inst().getServerWorld();
    }
  }

  @CmdArgNames({"red", "green", "blue", "alpha"})
  @ConsoleDoc(
      description = "Set the color of the sky. Params are expected to be between 0 and 1",
      paramDescriptions = {"red", "green", "blue", "alpha"})
  public void skyColor(float r, float g, float b, float a) {
    Color skylight = getWorld().getWorldTime().getBaseColor();
    skylight.set(r, g, b, a);
    logger.success("Sky color changed to " + skylight);
  }

  @CmdArgNames("color")
  @ConsoleDoc(
      description = "Set the color of the sky",
      paramDescriptions = {"Name of color"})
  public void skyColor(String colorName) {
    if (Settings.client) {
      Color skylight = getWorld().getWorldTime().getBaseColor();
      try {
        Color color = (Color) Reflection.getStaticField(Color.class, colorName.toUpperCase());
        skylight.set(color);
        logger.log("Sky color changed to " + colorName.toLowerCase() + " (" + color + ")");
      } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
        logger.logf(LogLevel.ERROR, "Unknown color '%s'", colorName);
      }
    } else {
      logger.log(LogLevel.ERROR, "Cannot change the color of the sky as graphics are not enabled");
    }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggle rendering of lights")
  public void lights() {
    Settings.renderLight = !Settings.renderLight;
    if (Settings.renderLight) {
      getWorld().getRender().update();
    }
    logger.log(
        LogLevel.SUCCESS, "Lighting is now " + (Settings.renderLight ? "enabled" : "disabled"));
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Reload all loaded chunks if unloading is allowed")
  public void reload() {
    reload(false);
  }

  @CmdArgNames("force")
  @ClientsideOnly
  @ConsoleDoc(
      description = "Reload all loaded chunks",
      paramDescriptions = "Force unloading of chunks even when unloading is disallowed")
  public void reload(boolean force) {
    getWorld().reload(force);
    logger.log(LogLevel.SUCCESS, "All chunks have been reloaded");
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggle flight for player")
  public void fly() {
    Player player = ClientMain.inst().getPlayer();
    if (player == null) {
      logger.error("CMD", "Failed to find any players");
      return;
    }
    player.setFlying(!player.isFlying());
    logger.log(LogLevel.SUCCESS, "Player is now " + (player.isFlying() ? "" : "not ") + "flying");
  }

  @ConsoleDoc(
      description =
          "Pauses the world ticker. This includes Box2D world updates, light updates, unloading of chunks,"
              + " entity updates and chunks update")
  public void pause() {
    Ticker ticker = getWorld().getWorldTicker();
    if (ticker.isPaused()) {
      logger.log(LogLevel.ERROR, "World is already paused");
    } else {
      ticker.pause();
      logger.log(LogLevel.SUCCESS, "World is now paused");
    }
  }

  @ConsoleDoc(
      description =
          "Resumes the world ticker. This includes Box2D world updates, light updates, unloading of chunks,"
              + " entity updates and chunks update")
  public void resume() {
    World world = getWorld();
    Ticker ticker = getWorld().getWorldTicker();
    if (ticker.isPaused()) {
      world.getWorldTicker().resume();
      world.getRender().update();
      logger.log(LogLevel.SUCCESS, "World is now resumed");
    } else {
      logger.log(LogLevel.ERROR, "World is not paused");
    }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggles debug rendering of Box2D objects")
  public void debug() {
    Settings.renderBox2dDebug = !Settings.renderBox2dDebug;
    Settings.debug = true;
    logger.log(
        LogLevel.SUCCESS,
        "Debug rendering for Box2D is now " + (Settings.renderBox2dDebug ? "enabled" : "disabled"));
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggles smoothed camera movement when following a player")
  public void lerp() {
    Settings.enableCameraFollowLerp = !Settings.enableCameraFollowLerp;
    logger.log(
        LogLevel.SUCCESS,
        "Camera lerp is now " + (Settings.enableCameraFollowLerp ? "enabled" : "disabled"));
  }

  @CmdArgNames({"x", "y"})
  @ClientsideOnly
  @ConsoleDoc(
      description = "Teleport to given world coordinate",
      paramDescriptions = {"World x coordinate", "World y coordinate"})
  public void tp(float worldX, float worldY) {
    if (!Settings.client) {
      return;
    }
    WorldRender render = getWorld().getRender();
    var worldBody = getWorld().getWorldBody();
    render.getCamera().position.x = worldX * Block.BLOCK_SIZE + worldBody.getWorldOffsetX();
    render.getCamera().position.y = worldY * Block.BLOCK_SIZE + worldBody.getWorldOffsetY();
    render.update();
    logger.logf(LogLevel.SUCCESS, "Teleported camera to (% .2f,% .2f)", worldX, worldY);

    Player player = ClientMain.inst().getPlayer();
    if (player == null) {
      logger.error("CMD", "Failed to find any players");
      return;
    }
    player.teleport(worldX, worldY, false);
    logger.logf(LogLevel.SUCCESS, "Teleported player to (% .2f,% .2f)", worldX, worldY);
  }

  @CmdArgNames({"quality"})
  @ClientsideOnly
  @ConsoleDoc(
      description = "The quality of the light. To disable light use command 'light'",
      paramDescriptions = "Quality of light, between 0 and 4")
  public void lightQuality(int quality) {
    if (quality > 4) {
      logger.warn("Quality given is greater than the maximum. It has been set to 4");
      quality = 4;
    } else if (quality < 0) {
      logger.warn("Quality given is a negative number. It has been set to 0");
      quality = 0;
    }
    getWorld().getRender().getRayHandler().setBlurNum(quality);
    logger.success("Light quality is now " + quality);
  }

  @CmdArgNames({"modus"})
  @ClientsideOnly
  @ConsoleDoc(
      description = "Set how much information to show",
      paramDescriptions = "normal (default), debug or none")
  public void hud(String modusName) {
    var screen = ClientMain.inst().getScreen();
    if (!(screen instanceof WorldScreen)) {
      logger.log(LogLevel.ERROR, "Not currently in a world, cannot change hud");
      return;
    }

    try {
      HUDRenderer.HUDModus modus = HUDRenderer.HUDModus.valueOf(modusName.toUpperCase());
      ((WorldScreen) screen).getHud().setModus(modus);
    } catch (IllegalArgumentException e) {
      logger.log(LogLevel.ERROR, "Unknown HUD modus '" + modusName + "'");
    }
  }

  @CmdArgNames({"zoom level"})
  @ClientsideOnly
  @ConsoleDoc(
      description = "Change the zoom level of the world camera",
      paramDescriptions = "The new zoom level, min is " + WorldRender.MIN_ZOOM)
  public void zoom(float zoom) {
    if (zoom < WorldRender.MIN_ZOOM) {
      logger.warn(
          "Given zoom level (%.3f) is less than the minimum % .3f", zoom, WorldRender.MIN_ZOOM);
      zoom = WorldRender.MIN_ZOOM;
    }
    WorldRender render = getWorld().getRender();
    render.getCamera().zoom = Math.max(zoom, WorldRender.MIN_ZOOM);
    render.update();
    logger.success("Zoom level is now " + zoom);
  }

  @HiddenCommand
  @ClientsideOnly
  public void paint() {
    Player player = ClientMain.inst().getPlayer();
    if (player == null) {
      logger.error("CMD", "Failed to find any players");
      return;
    }
    for (Block block : player.touchingBlocks()) {
      block.setBlock(Material.TORCH);
    }
  }

  @CmdArgNames({"x", "y", "width", "height"})
  @ConsoleDoc(
      description =
          "Spawn a generic static entity at the given location with the given width and height",
      paramDescriptions = {"worldX", "worldY", "width", "height"})
  public void ent(float worldX, float worldY, int width, int height) {
    final World world = getWorld();
    var entity = new GenericEntity(world, worldX, worldY, width, height);
    if (entity.isInvalid()) {
      logger.error(
          "GEN ENT",
          "Failed to create an entity at (% 7.2f,% 7.2f) with width %d and height %d",
          worldX,
          worldY,
          width,
          height);
    } else {
      world.addEntity(entity);
      logger.logf(
          "Created an entity at (% 7.2f,% 7.2f) with width %d and height %d",
          worldX, worldY, width, height);
    }
  }

  @ConsoleDoc(description = "Kill all non-player entities")
  public void killall() {
    World world = getWorld();
    int entities = 0;
    synchronized (BOX2D_LOCK) {
      for (Entity entity : world.getEntities()) {
        if (entity instanceof Player) {
          continue;
        }
        entities++;
        world.removeEntity(entity);
      }
    }
    logger.log(LogLevel.SUCCESS, "Killed " + entities + " entities");
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Get the brush sizes")
  public void brush() {
    Player player = ClientMain.inst().getPlayer();
    if (player == null) {
      logger.error("CMD", "Failed to find any players");
      return;
    }
    EntityControls controls = ClientMain.inst().getPlayer().getControls();
    if (controls == null) {
      logger.error("CMD", "The main player does not have any controls");
    } else {
      logger.logf(
          LogLevel.SUCCESS,
          "Brush size for player are now %.2f blocks for breaking and %.2f blocks for placing",
          controls.getBreakBrushSize(),
          controls.getPlaceBrushSize());
    }
  }

  @CmdArgNames({"type", "size"})
  @ClientsideOnly
  @ConsoleDoc(
      description = "Set the brush size of the mouse",
      paramDescriptions = {
        "Type of brush to change, can be 'break' and 'place'",
        "New brush size, positive integer"
      })
  public void brush(String type, float size) {
    Player player = ClientMain.inst().getPlayer();
    if (player == null) {
      logger.error("CMD", "Failed to find any players");
      return;
    }
    EntityControls controls = ClientMain.inst().getPlayer().getControls();
    if (controls == null) {
      logger.error("CMD", "The main player does not have any controls");
      return;
    }

    if ("break".equalsIgnoreCase(type)) {
      controls.setBreakBrushSize(size);
    } else if ("place".equalsIgnoreCase(type)) {
      controls.setPlaceBrushSize(size);
    } else {
      logger.error("CMD", "Valid brush types are 'break' and 'place'");
      return;
    }
    brush();
  }

  @CmdArgNames({"scale"})
  @ConsoleDoc(description = "How fast the time flows", paramDescriptions = "The new scale of time")
  public void timescale(float scale) {
    float old = getWorld().getWorldTime().getTimeScale();
    getWorld().getWorldTime().setTimeScale(scale);
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
      // There is a chance this method is selected before  the other time method
      time = Float.parseFloat(timeOfDay);
    } catch (NumberFormatException ignored) {

      switch (timeOfDay.toLowerCase()) {
        case "day", "dawn", "sunrise" -> time = WorldTime.SUNRISE_TIME;
        case "midday", "noon" -> time = WorldTime.MIDDAY_TIME;
        case "dusk", "sunset" -> time = WorldTime.SUNSET_TIME;
        case "midnight", "night" -> time = WorldTime.MIDNIGHT_TIME;
        case "end" -> time = Integer.MAX_VALUE;
        default -> {
          logger.error("CMD", "Unknown time of day, try sunrise, midday, sunset or midnight");
          return;
        }
      }
    }

    // call the other time function
    time(time);
  }

  @CmdArgNames({"time"})
  @ConsoleDoc(description = "Set the current time", paramDescriptions = "The new time")
  public void time(float time) {
    float old = getWorld().getWorldTime().getTime();
    getWorld().getWorldTime().setTime(time);
    logger.success("Changed time from % .3f to % .3f", old, time);
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Reset camera and zoom")
  public void reset() {
    Player player = ClientMain.inst().getPlayer();
    if (player == null) {
      logger.error("CMD", "Failed to find any players");
      return;
    }
    World world = getWorld();
    WorldRender render = world.getRender();
    render.getCamera().zoom = 1f;
    render.getCamera().position.x = player.getPosition().x * Block.BLOCK_SIZE;
    render.getCamera().position.y = player.getPosition().y * Block.BLOCK_SIZE;
    if (world.getInput() != null) {
      world.getInput().setLockedOn(true);
    }
    render.update();
  }

  @HiddenCommand
  @ClientsideOnly
  @CmdArgNames({"dx", "dy"})
  @ConsoleDoc(description = "Shift world offset")
  public void swo(float x, float y) {
    getWorld().getWorldBody().shiftWorldOffset(x, y);
  }

  @HiddenCommand
  @ConsoleDoc(description = "check dangling entities")
  public void cde() {
    synchronized (BOX2D_LOCK) {
      var world = getWorld();
      final com.badlogic.gdx.physics.box2d.World worldBox2dWorld =
          world.getWorldBody().getBox2dWorld();
      var bodies = new Array<Body>(worldBox2dWorld.getBodyCount());
      worldBox2dWorld.getBodies(bodies);

      int invalid = 0;
      for (Body body : bodies) {
        final Object userData = body.getUserData();
        if (userData != null && userData instanceof Entity entity) {
          if (world.containsEntity(entity.getUuid())) {
            continue;
          }
          invalid++;
          logger.error(
              "Entity",
              "Found entity not added to the world! "
                  + entity.simpleName()
                  + " "
                  + entity.hudDebug());
        }
      }
      if (invalid == 0) {
        logger.success("No invalid bodies found!");
      }
    }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Spawn a new player at the worlds spawn")
  public void spawnPlayer() {
    getWorld().createNewPlayer(UUID.randomUUID());
  }

  @ConsoleDoc(description = "Save the world server side")
  public void save() {
    getWorld().save();
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Disconnect from the server")
  public void quit() {
    disconnect();
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Disconnect from the server")
  public void disconnect() {
    final ServerClient client = ClientMain.inst().getServerClient();
    if (client != null) {
      client.ctx.writeAndFlush(
          PacketExtraKt.serverBoundClientDisconnectPacket(client, "Disconnect command"));
    }
    ConnectingScreen.INSTANCE.setInfo("Disconnected");
    Main.inst()
        .getScheduler()
        .scheduleSync(() -> ClientMain.inst().setScreen(MainMenuScreen.INSTANCE), 50L);
  }
}
