package no.elg.infiniteBootleg.world.subgrid.enitites;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.render.WorldRender.LIGHT_LOCK;
import static no.elg.infiniteBootleg.world.subgrid.InvalidSpawnAction.PUSH_UP;

import box2dLight.ConeLight;
import box2dLight.Light;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.input.EntityControls;
import no.elg.infiniteBootleg.input.KeyboardControls;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.subgrid.InvalidSpawnAction;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import org.jetbrains.annotations.NotNull;


public class Player extends LivingEntity {

    public static final String PLAYER_REGION_NAME = "player";

    private TextureRegion region;
    private EntityControls controls;
    private Light torchLight;

    private final Vector2 tmpAngle = new Vector2();

    public Player(@NotNull World world) {
        super(world, 0, 0);
        if (isInvalid()) {
            return;
        }
        region = new TextureRegion(Main.inst().getEntityAtlas().findRegion(PLAYER_REGION_NAME));
        controls = new KeyboardControls(world.getRender(), this);
        synchronized (LIGHT_LOCK) {
            torchLight = new ConeLight(Main.inst().getWorld().getRender().getRayHandler(), 64, Color.TAN, 48, 5, 5, 0, 30);
            torchLight.setStaticLight(true);
            torchLight.setContactFilter(World.LIGHT_FILTER);
            torchLight.setSoftnessLength(World.POINT_LIGHT_SOFTNESS_LENGTH);
        }
    }

    @Override
    public TextureRegion getTextureRegion() {
        return region;
    }

    @Override
    public int getWidth() {
        return 2 * BLOCK_SIZE - 1;
    }

    @Override
    public int getHeight() {
        return 4 * BLOCK_SIZE - 1;
    }

    @Override
    public synchronized void dispose() {
        if (isInvalid()) {
            Main.logger().error("Player", "Tried to dispose disposed player");
            return;
        }
        super.dispose();
        controls.dispose();
        synchronized (LIGHT_LOCK) {
            torchLight.remove();
        }
    }

    @Override
    public InvalidSpawnAction invalidSpawnLocationAction() {
        return PUSH_UP;
    }

    @Override
    public void tick() {
        if (torchLight == null) {
            return;
        }
        super.tick();

        Vector2 pos = getPhysicsPosition();
        float angle = tmpAngle.set(Main.inst().getMouse()).sub(getPosition()).angleDeg();
        synchronized (LIGHT_LOCK) {
            torchLight.setDirection(angle);
            torchLight.setPosition(pos);
        }
    }

    @Override
    @NotNull
    public EntityControls getControls() {
        return controls;
    }
}
