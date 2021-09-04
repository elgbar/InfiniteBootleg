package no.elg.infiniteBootleg.world.subgrid.enitites;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.render.WorldRender.LIGHT_LOCK;
import static no.elg.infiniteBootleg.world.subgrid.InvalidSpawnAction.PUSH_UP;

import box2dLight.ConeLight;
import box2dLight.Light;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.google.common.base.Preconditions;
import java.util.UUID;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.input.EntityControls;
import no.elg.infiniteBootleg.input.KeyboardControls;
import no.elg.infiniteBootleg.protobuf.Proto;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.WorldRender;
import no.elg.infiniteBootleg.world.subgrid.InvalidSpawnAction;
import no.elg.infiniteBootleg.world.subgrid.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class Player extends LivingEntity {

    public static final String PLAYER_REGION_NAME = "player";

    @NotNull
    private static final TextureRegion TEXTURE_REGION;
    @Nullable
    private EntityControls controls;
    @NotNull
    private final Light torchLight;

    public Player(@NotNull World world, Proto.@NotNull Entity protoEntity) {
        super(world, protoEntity);
        if (isInvalid()) {
            return;
        }

        Preconditions.checkArgument(protoEntity.hasPlayer(), "Player does not contain player data");
        final Proto.Entity.Player protoPlayer = protoEntity.getPlayer();
        setTorchAngle(protoPlayer.getTorchAngleDeg());
        if (protoPlayer.getControlled()) {
            Main.inst().setPlayer(this, false);
        }
    }

    public Player(@NotNull World world, float worldX, float worldY) {
        super(world, worldX, worldY, UUID.randomUUID());
        if (isInvalid()) {
            return;
        }
        giveControls();
    static {
        TEXTURE_REGION = new TextureRegion(Main.inst().getEntityAtlas().findRegion(PLAYER_REGION_NAME));
    }

    {
        synchronized (LIGHT_LOCK) {
            torchLight = new ConeLight(Main.inst().getWorld().getRender().getRayHandler(), 64, Color.TAN, 48, 5, 5, 0, 30);
            torchLight.setStaticLight(true);
            torchLight.setContactFilter(World.LIGHT_FILTER);
            torchLight.setSoftnessLength(World.POINT_LIGHT_SOFTNESS_LENGTH);
        }
    }

    public synchronized void giveControls() {
        if (controls == null) {
            Main.inst().getConsoleLogger().debug("PLR", "Giving control to " + hudDebug());
            controls = new KeyboardControls(getWorld().getRender(), this);
        }
        else {
            Main.inst().getConsoleLogger().warn("PLR", "Tried to give control to a player already with control" + hudDebug());
        }
    }

    public synchronized void removeControls() {
        if (controls != null) {
            Main.inst().getConsoleLogger().debug("PLR", "Removing control from " + hudDebug());
            controls.dispose();
            controls = null;
        }
        else {
            Main.inst().getConsoleLogger().warn("PLR", "Tried to remove control from a player without control" + hudDebug());
        }
    }

    @Override
    public synchronized boolean hasControls() {
        return controls != null;
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

    public void setTorchAngle(float angleDeg) {
        synchronized (WorldRender.LIGHT_LOCK) {
            torchLight.setDirection(angleDeg);
        }
    }

    @Override
    public Proto.Entity.Builder save() {
        final Proto.Entity.Builder builder = super.save();
        final Proto.Entity.Player.Builder playerBuilder = Proto.Entity.Player.newBuilder();

        playerBuilder.setTorchAngleDeg(torchLight.getDirection());
        playerBuilder.setControlled(hasControls());

        builder.setPlayer(playerBuilder.build());
        return builder;
    }

    @Override
    public synchronized void dispose() {
        if (isInvalid()) {
            Main.logger().error("Player", "Tried to dispose disposed player");
            return;
        }
        super.dispose();
        if (hasControls()) {
            removeControls();
        }
        synchronized (LIGHT_LOCK) {
            torchLight.remove();
        }
    }

    @Override
    public InvalidSpawnAction invalidSpawnLocationAction() {
        return PUSH_UP;
    }

    public Light getTorchLight() {
        return torchLight;
    }

    @Override
    public void tick() {
        super.tick();
        Vector2 pos = getPhysicsPosition();
        synchronized (LIGHT_LOCK) {
            torchLight.setPosition(pos);
        }
    }

    @Override
    @Nullable
    public EntityControls getControls() {
        return controls;
    }

    @Override
    protected @NotNull Proto.Entity.EntityType getEntityType() {
        return Proto.Entity.EntityType.PLAYER;
    }
}
