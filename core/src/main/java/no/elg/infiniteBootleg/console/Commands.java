package no.elg.infiniteBootleg.console;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;
import com.strongjoshua.console.CommandExecutor;
import com.strongjoshua.console.LogLevel;
import com.strongjoshua.console.annotation.ConsoleDoc;
import com.strongjoshua.console.annotation.HiddenCommand;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.events.api.EventEvent;
import no.elg.infiniteBootleg.events.api.EventManager;
import no.elg.infiniteBootleg.events.api.EventsTracker;
import no.elg.infiniteBootleg.input.EntityControls;
import no.elg.infiniteBootleg.screen.HUDRenderer;
import no.elg.infiniteBootleg.screens.ConnectingScreen;
import no.elg.infiniteBootleg.screens.MainMenuScreen;
import no.elg.infiniteBootleg.screens.WorldScreen;
import no.elg.infiniteBootleg.server.PacketExtraKt;
import no.elg.infiniteBootleg.server.ServerClient;
import no.elg.infiniteBootleg.util.ReflectionUtil;
import no.elg.infiniteBootleg.util.Ticker;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.Chunk;
import no.elg.infiniteBootleg.world.ClientWorld;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.ClientWorldRender;
import no.elg.infiniteBootleg.world.render.HeadlessWorldRenderer;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import no.elg.infiniteBootleg.world.subgrid.enitites.GenericEntity;
import no.elg.infiniteBootleg.world.subgrid.enitites.Player;
import no.elg.infiniteBootleg.world.time.WorldTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
@SuppressWarnings("unused")
public class Commands extends CommandExecutor {

  private final ConsoleLogger logger;

  public Commands(@NotNull ConsoleLogger logger) {
    this.logger = logger;
  }

  @Nullable
  private World getWorld() {
    var world = Main.inst().getWorld();
    if (world == null) {
      logger.error("CMD", "Failed to find the current world");
    }
    return world;
  }

  @Nullable
  private ClientWorld getClientWorld() {
    var world = ClientMain.inst().getWorld();
    if (world == null) {
      logger.error("CMD", "Failed to find world");
    }
    return world;
  }

  @Nullable
  private Player getSPPlayer() {
    Player player = ClientMain.inst().getPlayer();
    if (player == null) {
      logger.error("CMD", "Failed to find any players");
    }
    return player;
  }

  @CmdArgNames({"red", "green", "blue", "alpha"})
  @ConsoleDoc(
      description = "Set the color of the sky. Params are expected to be between 0 and 1",
      paramDescriptions = {"red", "green", "blue", "alpha"})
  public void skyColor(float r, float g, float b, float a) {
    World world = getWorld();
    if (world == null) return;
    Color skylight = world.getWorldTime().getBaseColor();
    skylight.set(r, g, b, a);
    logger.success("Sky color changed to " + skylight);
  }

  @ClientsideOnly
  @CmdArgNames("color")
  @ConsoleDoc(
      description = "Set the color of the sky",
      paramDescriptions = {"Name of color"})
  public void skyColor(String colorName) {
    World world = getWorld();
    if (world == null) return;
    Color skylight = world.getWorldTime().getBaseColor();
    try {
      Color color = (Color) ReflectionUtil.getStaticField(Color.class, colorName.toUpperCase());
      skylight.set(color);
      logger.log("Sky color changed to " + colorName.toLowerCase() + " (" + color + ")");
    } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
      logger.logf(LogLevel.ERROR, "Unknown color '%s'", colorName);
    }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggle rendering of lights")
  public void lights() {
    Settings.renderLight = !Settings.renderLight;

    World world = getWorld();
    if (world == null) return;
    if (Settings.renderLight) {
      world.getRender().update();
    }
    for (Chunk chunk : world.getLoadedChunks()) {
      chunk.dirty();
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
    World world = getWorld();
    if (world == null) return;
    world.reload(force);
    logger.log(LogLevel.SUCCESS, "All chunks have been reloaded");
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggle flight for player")
  public void fly() {
    Player player = getSPPlayer();
    if (player == null) {
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
    World world = getWorld();
    if (world == null) return;
    Ticker ticker = world.getWorldTicker();
    if (ticker.isPaused()) {
      logger.error("World is already paused");
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
    if (world == null) return;
    Ticker ticker = world.getWorldTicker();
    if (ticker.isPaused()) {
      world.getWorldTicker().resume();
      world.getRender().update();
      logger.log(LogLevel.SUCCESS, "World is now resumed");
    } else {
      logger.error("World is not paused");
    }
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggles debug rendering of Box2D objects")
  public void debBox() {
    Settings.renderBox2dDebug = !Settings.renderBox2dDebug;
    Settings.debug = true;
    logger.log(
        LogLevel.SUCCESS,
        "Debug rendering for Box2D is now " + (Settings.renderBox2dDebug ? "enabled" : "disabled"));
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggles debug rendering of chunk bounds")
  public void debChu() {
    Settings.renderChunkBounds = !Settings.renderChunkBounds;
    Settings.debug = true;
    logger.log(
        LogLevel.SUCCESS,
        "Debug rendering of chunks is now " + (Settings.renderBox2dDebug ? "enabled" : "disabled"));
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggles debug rendering of entity lighting")
  public void debEntLit() {
    Settings.debugEntityLight = !Settings.debugEntityLight;
    Settings.debug = true;
    logger.log(
        LogLevel.SUCCESS,
        "Debug rendering of entity light is now "
            + (Settings.debugEntityLight ? "enabled" : "disabled"));
    if (Settings.debugEntityLight) {
      logger.log(
          LogLevel.DEFAULT,
          "A white box is rendered over the block each entity source their brightness from");
    }
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
    var clientWorld = getClientWorld();
    if (clientWorld == null) {
      return;
    }
    ClientWorldRender render = clientWorld.getRender();
    var worldBody = clientWorld.getWorldBody();
    render.getCamera().position.x = worldX * Block.BLOCK_SIZE + worldBody.getWorldOffsetX();
    render.getCamera().position.y = worldY * Block.BLOCK_SIZE + worldBody.getWorldOffsetY();
    render.update();
    logger.logf(LogLevel.SUCCESS, "Teleported camera to (% .2f,% .2f)", worldX, worldY);

    Player player = getSPPlayer();
    if (player == null) {
      return;
    }
    player.teleport(worldX, worldY, false);
    logger.logf(LogLevel.SUCCESS, "Teleported player to (% .2f,% .2f)", worldX, worldY);
  }

  @CmdArgNames({"mode"})
  @ClientsideOnly
  @ConsoleDoc(
      description = "Toggle how much information to show",
      paramDescriptions = "block, debug, graph, mindebug, or none")
  public void hud(String modusName) {
    var screen = ClientMain.inst().getScreen();
    if (!(screen instanceof WorldScreen worldScreen)) {
      logger.error("Not currently in a world, cannot change hud");
      return;
    }
    var hud = worldScreen.getHud();

    int mode =
        switch (modusName) {
          case "block" -> HUDRenderer.DISPLAY_CURRENT_BLOCK;
          case "mindebug" -> HUDRenderer.DISPLAY_MINIMAL_DEBUG;
          case "debug" -> HUDRenderer.DISPLAY_DEBUG;
          case "graph" -> HUDRenderer.DISPLAY_GRAPH_FPS;
          case "none" -> HUDRenderer.DISPLAY_NOTHING;
          default -> -1;
        };
    if (mode < 0) {
      logger.error("Unknown HUD modus '" + modusName + "'");
    } else if (mode == 0) {
      hud.displayNothing();
    } else {
      hud.toggleMode(mode);
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
    var clientWorld = getClientWorld();
    if (clientWorld == null) {
      return;
    }
    ClientWorldRender render = clientWorld.getRender();
    render.getCamera().zoom = Math.max(zoom, WorldRender.MIN_ZOOM);
    render.update();
    logger.success("Zoom level is now " + zoom);
  }

  @HiddenCommand
  @ClientsideOnly
  public void paint() {
    Player player = getSPPlayer();
    if (player == null) {
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
    World world = getWorld();
    if (world == null) {
      return;
    }

    var entity = new GenericEntity(world, worldX, worldY, width, height);
    if (entity.isDisposed()) {
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
    if (world == null) {
      return;
    }
    int entities = 0;
    for (Entity entity : world.getEntities()) {
      if (entity instanceof Player) {
        continue;
      }
      entities++;
      world.removeEntity(entity);
    }
    logger.log(LogLevel.SUCCESS, "Killed " + entities + " entities");
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Get the brush sizes")
  public void brush() {
    Player player = getSPPlayer();
    if (player == null) {
      return;
    }
    EntityControls controls = player.getControls();
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
    Player player = getSPPlayer();
    if (player == null) {
      return;
    }
    EntityControls controls = player.getControls();
    if (controls == null) {
      logger.error("CMD", "The main player does not have any controls");
      return;
    }
    if (type == null) {
      logger.error("CMD", "Valid brush types are 'break' and 'place'");
      return;
    }
    type = type.toLowerCase(Locale.ROOT);

    if (type.startsWith("b")) {
      controls.setBreakBrushSize(size);
    } else if (type.startsWith("p")) {
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
    var world = getWorld();
    if (world == null) {
      return;
    }
    WorldTime worldTime = world.getWorldTime();
    float old = worldTime.getTimeScale();
    worldTime.setTimeScale(scale);
    logger.success("Changed time scale from % .3f to % .3f", old, scale);

    PacketExtraKt.sendDuplexPacket(
        () -> PacketExtraKt.clientBoundWorldSettings(null, null, scale),
        client -> PacketExtraKt.serverBoundWorldSettings(client, null, null, scale));
  }

  @ConsoleDoc(description = "Toggle if time ticks or not")
  public void toggleTime() {
    Settings.dayTicking = !Settings.dayTicking;

    logger.success("Time is now " + (Settings.dayTicking ? "" : "not ") + "ticking");

    PacketExtraKt.sendDuplexPacket(
        () -> PacketExtraKt.clientBoundWorldSettings(null, null, Settings.dayTicking ? 1f : 0f),
        client ->
            PacketExtraKt.serverBoundWorldSettings(
                client, null, null, Settings.dayTicking ? 1f : 0f));
  }

  @CmdArgNames({"time of day"})
  @ConsoleDoc(
      description = "Set the current time",
      paramDescriptions = "Time of day such as day, noon, dusk, night")
  public void time(String timeOfDay) {
    float time;
    try {
      // There is a chance this method is selected before  the other time method
      time = Float.parseFloat(timeOfDay);
    } catch (NumberFormatException ignored) {

      switch (timeOfDay.toLowerCase()) {
        case "dawn" -> time = WorldTime.DAWN_TIME;
        case "day", "sunrise" -> time = WorldTime.SUNRISE_TIME;
        case "midday", "noon" -> time = WorldTime.MIDDAY_TIME;
        case "sunset" -> time = WorldTime.SUNSET_TIME;
        case "dusk" -> time = WorldTime.DUSK_TIME;
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
  @ConsoleDoc(
      description = "Set the current time",
      paramDescriptions = "The new time as a number with sunrise as 0, noon as 90, dusk as 180 etc")
  public void time(float time) {
    var world = getWorld();
    if (world == null) {
      return;
    }
    WorldTime worldTime = world.getWorldTime();
    float old = worldTime.getTime();
    worldTime.setTime(time);

    PacketExtraKt.sendDuplexPacket(
        () -> PacketExtraKt.clientBoundWorldSettings(null, time, null),
        client -> PacketExtraKt.serverBoundWorldSettings(client, null, time, null));
    logger.success("Changed time from % .3f to % .3f", old, time);
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Reset camera and zoom")
  public void reset() {
    Player player = getSPPlayer();
    if (player == null) {
      return;
    }
    var world = getClientWorld();
    if (world == null) {
      return;
    }
    ClientWorldRender render = world.getRender();
    render.getCamera().zoom = 1f;
    render.getCamera().position.x = player.getPosition().x * Block.BLOCK_SIZE;
    render.getCamera().position.y = player.getPosition().y * Block.BLOCK_SIZE;
    world.getInput().setLockedOn(true);
    render.update();
  }

  @HiddenCommand
  @ClientsideOnly
  @CmdArgNames({"dx", "dy"})
  @ConsoleDoc(description = "Shift world offset")
  public void swo(float x, float y) {
    var world = getWorld();
    if (world == null) {
      return;
    }
    world.getWorldBody().shiftWorldOffset(x, y);
  }

  @HiddenCommand
  @ConsoleDoc(description = "check dangling entities")
  public void cde() {
    var world = getWorld();
    if (world == null) {
      return;
    }
    world.postBox2dRunnable(
        () -> {
          final com.badlogic.gdx.physics.box2d.World worldBox2dWorld =
              world.getWorldBody().getBox2dWorld();
          var bodies = new Array<Body>(worldBox2dWorld.getBodyCount());
          worldBox2dWorld.getBodies(bodies);

          int invalid = 0;
          for (Body body : bodies) {
            final Object userData = body.getUserData();
            if (userData instanceof Entity entity) {
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
        });
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Spawn a new player at the worlds spawn")
  public void spawnPlayer() {
    var world = getWorld();
    if (world == null) {
      return;
    }
    world.createNewPlayer(UUID.randomUUID());
  }

  @ConsoleDoc(description = "Save the world server side")
  public void save() {
    var world = getWorld();
    if (world == null) {
      return;
    }
    world.save();
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Toggle vsync")
  @CmdArgNames({"enable"})
  public void vsync(boolean enable) {
    Gdx.graphics.setVSync(enable);
    logger.success("VSync is now " + (enable ? "enabled" : "disabled"));
  }

  @ClientsideOnly
  @ConsoleDoc(description = "Set max FPS, if < 0 there is no limit. ")
  @CmdArgNames({"fps"})
  public void maxFPS(int fps) {
    Gdx.graphics.setForegroundFPS(fps);
    logger.success("Max foreground fps is now " + fps);
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
        .scheduleSync(50L, () -> ClientMain.inst().setScreen(MainMenuScreen.INSTANCE));
  }

  @ConsoleDoc(description = "Some debug info")
  public void chunkInfo() {
    Main.logger().log("Debug chunk Info");
    var world = getWorld();
    if (world == null) {
      return;
    }
    Main.logger().log("Loaded chunks: " + world.getLoadedChunks().size);
    for (Player player : world.getPlayers()) {
      var worldRenderer = world.getRender();
      if (worldRenderer instanceof ClientWorldRender clientWr) {
        var view = clientWr.getChunksInView();
        Main.logger()
            .log(
                "Player "
                    + player.getName()
                    + " from "
                    + view.getHorizontalStart()
                    + ", "
                    + view.getVerticalStart()
                    + " to "
                    + view.getHorizontalEnd()
                    + ","
                    + view.getVerticalEnd());
      } else if (worldRenderer instanceof HeadlessWorldRenderer headlessWr) {
        var view = headlessWr.getClient(player.getUuid());
        if (view == null) {
          Main.logger()
              .error("HeadlessWorldRenderer", "No world renderer for player " + player.hudDebug());
          continue;
        }
        Main.logger()
            .log(
                "Player "
                    + player.getName()
                    + " view center: ("
                    + view.getCenterX()
                    + ","
                    + view.getCenterY()
                    + ") from "
                    + view.getHorizontalStart()
                    + ", "
                    + view.getVerticalStart()
                    + " to "
                    + view.getHorizontalEnd()
                    + ","
                    + view.getVerticalEnd());
      }
    }

    Main.logger()
        .log(
            "Chunk pos: \n"
                + Arrays.stream(world.getLoadedChunks().items)
                    .sorted()
                    .map(
                        c ->
                            "("
                                + c.getChunkX()
                                + ", "
                                + c.getChunkY()
                                + ") in view? "
                                + !world.getRender().isOutOfView(c))
                    .collect(Collectors.joining("\n")));
  }

  @ConsoleDoc(description = "Toggle whether to track events")
  public void trackEvents() {
    boolean haveEventTracker = EventManager.INSTANCE.getEventTracker() != null;
    EventManager.INSTANCE.setEventTracker(haveEventTracker ? null : new EventsTracker(true));
    logger.success("Events are now " + (haveEventTracker ? "not " : "") + "tracked");
  }

  @ConsoleDoc(description = "Toggle whether to track events")
  public void printTrackedEvents() {
    EventsTracker eventTracker = EventManager.INSTANCE.getEventTracker();
    if (eventTracker == null) {
      logger.error("There is no active event tracker");
      return;
    }
    for (EventEvent recordedEvent : eventTracker.getRecordedEvents()) {
      logger.log(eventTracker.toString());
    }
  }
}
