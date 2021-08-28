package no.elg.infiniteBootleg.world.subgrid;

import no.elg.infiniteBootleg.Updatable;
import no.elg.infiniteBootleg.input.EntityControls;
import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

public abstract class LivingEntity extends Entity implements Updatable {

    public static final int DEFAULT_HEALTH = 10;
    public static final String DEFAULT_NAME = "LivingEntity";
    private int health = DEFAULT_HEALTH;
    private String name = DEFAULT_NAME;

    public LivingEntity(@NotNull World world, float x, float y) {
        super(world, x, y);
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

    @Override
    public void update() {
        getControls().update();
    }

    /**
     * @return How this entity should be controlled
     */
    @NotNull
    public abstract EntityControls getControls();

    @Override
    public @NotNull String hudDebug() {
        return "Name: " + getName() + " hp: " + getHealth() + "/" + DEFAULT_HEALTH;
    }

    @Override
    public String toString() {
        return "LivingEntity{name='" + name + "'} " + super.toString();
    }
}
