package no.elg.infiniteBootleg.world.subgrid;

import com.google.common.base.Preconditions;
import java.util.UUID;
import no.elg.infiniteBootleg.Updatable;
import no.elg.infiniteBootleg.input.EntityControls;
import no.elg.infiniteBootleg.protobuf.Proto;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LivingEntity extends Entity implements Updatable {

    public static final int DEFAULT_MAX_HEALTH = 10;
    public static final String DEFAULT_NAME = "LivingEntity";
    private int health = DEFAULT_MAX_HEALTH;
    private int maxHealth = DEFAULT_MAX_HEALTH;
    private String name = DEFAULT_NAME;

    public LivingEntity(@NotNull World world, Proto.@NotNull Entity protoEntity) {
        super(world, protoEntity);
        if (isInvalid()) {
            return;
        }
        Preconditions.checkArgument(protoEntity.hasLiving(), "Living entity does not contain living data");
        final Proto.Entity.Living protoLiving = protoEntity.getLiving();
        name = protoLiving.getName();
        health = protoLiving.getHealth();
        maxHealth = protoLiving.getMaxHealth();
    }

    public LivingEntity(@NotNull World world, float worldX, float worldY, @NotNull UUID uuid) {
        super(world, worldX, worldY, uuid);
    }

    @Override
    public void update() {
        final EntityControls controls = getControls();
        if (controls != null) {
            controls.update();
        }
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    /**
     * @return How this entity should be controlled (if at all)
     */
    @Nullable
    public abstract EntityControls getControls();

    @Override
    public Proto.Entity.Builder save() {
        final Proto.Entity.Builder builder = super.save();
        final Proto.Entity.Living.Builder livingBuilder = Proto.Entity.Living.newBuilder();

        livingBuilder.setHealth(health);
        livingBuilder.setMaxHealth(maxHealth);
        livingBuilder.setName(name);

        builder.setLiving(livingBuilder.build());
        return builder;
    }

    @Override
    public @NotNull String hudDebug() {
        return "Name: " + getName() + " hp: " + getHealth() + "/" + maxHealth;
    }

    @Override
    public String toString() {
        return "LivingEntity{name='" + name + "'} " + super.toString();
    }
}
