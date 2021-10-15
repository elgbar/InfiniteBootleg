package no.elg.infiniteBootleg.input;

import static com.badlogic.gdx.Input.Keys.DOWN;
import static com.badlogic.gdx.Input.Keys.F12;
import static com.badlogic.gdx.Input.Keys.F3;
import static com.badlogic.gdx.Input.Keys.F5;
import static com.badlogic.gdx.Input.Keys.F9;
import static com.badlogic.gdx.Input.Keys.LEFT;
import static com.badlogic.gdx.Input.Keys.RIGHT;
import static com.badlogic.gdx.Input.Keys.UP;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Updatable;
import no.elg.infiniteBootleg.screen.HUDRenderer;
import no.elg.infiniteBootleg.screen.HUDRenderer.HUDModus;
import no.elg.infiniteBootleg.screens.WorldScreen;
import no.elg.infiniteBootleg.util.Ticker;
import no.elg.infiniteBootleg.world.Block;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Elg
 */
public class WorldInputHandler extends InputAdapter implements Disposable, Updatable {

    public static final float SCROLL_SPEED = 0.25f;
    public static final float CAMERA_LERP = 2.5f;
    public static final float LERP_CUTOFF = 5f;
    private static final int CAMERA_SPEED = 100 * Block.BLOCK_SIZE;
    private final WorldRender worldRender;
    @Nullable
    private Entity following;
    private boolean lockedOn = true;

    public WorldInputHandler(@NotNull WorldRender world) {
        worldRender = world;
        Main.inst().getInputMultiplexer().addProcessor(this);
    }

    @Override
    public boolean keyDown(int keycode) {
        if (Main.inst().getConsole().isVisible()) {
            return false;
        }
        World world = Main.inst().getWorld();
        switch (keycode) {
            case F3:
                Screen screen = Main.inst().getScreen();
                if (screen instanceof WorldScreen worldScreen) {
                    HUDRenderer hud = worldScreen.getHud();
                    if (hud.getModus() == HUDModus.DEBUG) {
                        hud.setModus(HUDModus.NORMAL);
                    }
                    else {
                        hud.setModus(HUDModus.DEBUG);
                    }
                }
                break;
            case F5:
                world.save();
                break;
            case F9:
                world.reload(true);
                break;
            case F12:
                Ticker ticker = world.getWorldTicker();
                if (ticker.isPaused()) {
                    ticker.resume();
                    Main.logger().log("World", "Ticker resumed by F12");
                }
                else {
                    ticker.pause();
                    Main.logger().log("World", "Ticker paused by F12");
                }
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (Main.inst().getConsole().isVisible()) {
            return false;
        }
        OrthographicCamera camera = worldRender.getCamera();
        camera.zoom += ((amountX + amountY) / 2) * SCROLL_SPEED;
        if (camera.zoom < WorldRender.MIN_ZOOM) {
            camera.zoom = WorldRender.MIN_ZOOM;
        }
        else if (camera.zoom > WorldRender.MAX_ZOOM) {
            camera.zoom = WorldRender.MAX_ZOOM;
        }
        worldRender.update();
        return true;
    }

    public World getWorld() {
        return worldRender.getWorld();
    }

    @Override
    public void dispose() {
        Main.inst().getInputMultiplexer().removeProcessor(this);
    }

    /**
     * @return We are following a non-null, valid target, and is locked on
     */
    private boolean hasValidLockOn() {
        return following != null && !following.isInvalid() && lockedOn;
    }

    private void cameraFollowUpdate() {
        OrthographicCamera camera = worldRender.getCamera();
        if (hasValidLockOn()) {
            assert following != null;
            final Vector2 position = following.getPhysicsPosition();
            float x = position.x * Block.BLOCK_SIZE;
            float y = position.y * Block.BLOCK_SIZE;

            float dx = (x - camera.position.x) * CAMERA_LERP;
            float dy = (y - camera.position.y) * CAMERA_LERP;

            if (Math.abs(dx) > LERP_CUTOFF || Math.abs(dy) > LERP_CUTOFF) {
                camera.position.x += dx * Gdx.graphics.getDeltaTime();
                camera.position.y += dy * Gdx.graphics.getDeltaTime();
            }
            worldRender.update();
        }
    }

    private void teleportCamera() {
        OrthographicCamera camera = worldRender.getCamera();
        if (hasValidLockOn()) {
            assert following != null;
            final Vector2 position = following.getPhysicsPosition();

            camera.position.x = position.x * Block.BLOCK_SIZE;
            camera.position.y = position.y * Block.BLOCK_SIZE;
            worldRender.update();
        }
    }

    @Override
    public void update() {
        if (Main.inst().getConsole().isVisible()) {
            //keep following even when console is visible
            cameraFollowUpdate();
            return;
        }

        int vertical = (Gdx.input.isKeyPressed(UP) ? 1 : 0) - (Gdx.input.isKeyPressed(DOWN) ? 1 : 0);
        int horizontal = (Gdx.input.isKeyPressed(LEFT) ? 1 : 0) - (Gdx.input.isKeyPressed(RIGHT) ? 1 : 0);

        if (vertical == 0 && horizontal == 0) {
            //No input, we're still following the current entity
            cameraFollowUpdate();
        }
        else {
            OrthographicCamera camera = worldRender.getCamera();
            camera.position.x -= Gdx.graphics.getDeltaTime() * horizontal * CAMERA_SPEED * camera.zoom;
            camera.position.y += Gdx.graphics.getDeltaTime() * vertical * CAMERA_SPEED * camera.zoom;
            lockedOn = false;
            worldRender.update();
        }
    }

    public Entity getFollowing() {
        return following;
    }


    /**
     * Change who to follow, also automatically lock on and move the camera to the new following entity
     *
     * @param following
     *     What to follow, null if none
     */
    public void setFollowing(@Nullable Entity following) {
        if (following != null && following.isInvalid()) {
            Main.inst().getConsoleLogger().error("World Input", "Cannot pass a non-null invalid entity!");
            return;
        }
        //always update locked on status
        lockedOn = true;
        if (following == this.following) {
            //Do not teleport the camera, it looks very janky
            return;
        }
        this.following = following;
        teleportCamera();
    }

    /**
     * Only applies if {@link #getFollowing()} is not {@code null}
     */
    public boolean isLockedOn() {
        return lockedOn;
    }

    /**
     * Only applies if {@link #getFollowing()} is not {@code null}
     */
    public void setLockedOn(boolean lockedOn) {
        this.lockedOn = lockedOn;
    }
}
