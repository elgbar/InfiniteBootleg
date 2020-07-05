package no.elg.infiniteBootleg.input;

import com.badlogic.gdx.Gdx;
import static com.badlogic.gdx.Input.Keys.DOWN;
import static com.badlogic.gdx.Input.Keys.F3;
import static com.badlogic.gdx.Input.Keys.F5;
import static com.badlogic.gdx.Input.Keys.F9;
import static com.badlogic.gdx.Input.Keys.LEFT;
import static com.badlogic.gdx.Input.Keys.RIGHT;
import static com.badlogic.gdx.Input.Keys.UP;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.Updatable;
import no.elg.infiniteBootleg.screen.HUDRenderer;
import no.elg.infiniteBootleg.screen.HUDRenderer.HUDModus;
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
                HUDRenderer hud = Main.inst().getHud();
                if (hud.getModus() == HUDModus.DEBUG) {
                    hud.setModus(HUDModus.NORMAL);
                }
                else {
                    hud.setModus(HUDModus.DEBUG);
                }
                break;
            case F5:
                world.save();
                Main.logger().log("World", "World saved");
                break;
            case F9:
                world.load();
                Main.logger().log("World", "World reloaded last save");
                world.unloadChunks(true, false);
                world.getEntities().forEach(world::removeEntity);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public boolean scrolled(int amount) {
        if (Main.inst().getConsole().isVisible()) {
            return false;
        }
        OrthographicCamera camera = worldRender.getCamera();
        camera.zoom += amount * SCROLL_SPEED * camera.zoom;
        if (camera.zoom < WorldRender.MIN_ZOOM) {
            camera.zoom = WorldRender.MIN_ZOOM;
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

    @Override
    public void update() {
        if (Main.inst().getConsole().isVisible()) {
            return;
        }

        OrthographicCamera camera = worldRender.getCamera();

        int vertical = (Gdx.input.isKeyPressed(UP) ? 1 : 0) - (Gdx.input.isKeyPressed(DOWN) ? 1 : 0);
        int horizontal = (Gdx.input.isKeyPressed(LEFT) ? 1 : 0) - (Gdx.input.isKeyPressed(RIGHT) ? 1 : 0);

        if (vertical != 0 || horizontal != 0) {
            camera.position.x -= Gdx.graphics.getDeltaTime() * horizontal * CAMERA_SPEED * camera.zoom;
            camera.position.y += Gdx.graphics.getDeltaTime() * vertical * CAMERA_SPEED * camera.zoom;
            lockedOn = false;
            worldRender.update();
        }
        else if (following != null && following.isValid() && lockedOn) {
            float x = following.getPosition().x * Block.BLOCK_SIZE;
            float y = following.getPosition().y * Block.BLOCK_SIZE;

            if (Settings.enableCameraFollowLerp) {
                float dx = (x - camera.position.x) * CAMERA_LERP;
                float dy = (y - camera.position.y) * CAMERA_LERP;

                if (Math.abs(dx) > LERP_CUTOFF || Math.abs(dy) > LERP_CUTOFF) {
                    camera.position.x += dx * Gdx.graphics.getDeltaTime();
                    camera.position.y += dy * Gdx.graphics.getDeltaTime();
                }
            }
            else {
                camera.position.x = x;
                camera.position.y = y;
            }
            worldRender.update();
        }
    }

    public Entity getFollowing() {
        return following;
    }


    /**
     * Change who to follow, does not change if the camera <i>is</i> locked on
     *
     * @param following
     *     What to follow, null if none
     */
    public void setFollowing(@Nullable Entity following) {
        if (following == null || !following.isValid()) {
            throw new IllegalArgumentException("Cannot pass a non-null invalid entity!");
        }
        this.following = following;
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
