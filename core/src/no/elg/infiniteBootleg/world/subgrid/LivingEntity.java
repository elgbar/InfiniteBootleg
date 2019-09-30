package no.elg.infiniteBootleg.world.subgrid;

import no.elg.infiniteBootleg.input.EntityControls;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.Updatable;
import no.elg.infiniteBootleg.world.subgrid.enitites.AirResistanceEntity;
import org.jetbrains.annotations.NotNull;

public abstract class LivingEntity extends AirResistanceEntity implements Updatable {

    public static final int DEFAULT_HEALTH = 10;
    public static final String DEFAULT_NAME = "LivingEntity";
    private int health;
    private String name;

    public LivingEntity(@NotNull World world, float x, float y) {
        super(world, x, y);
        health = DEFAULT_HEALTH;
        name = DEFAULT_NAME;
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
    public String toString() {
        return "LivingEntity{" + "name='" + name + '\'' + "} " + super.toString();
    }
}
