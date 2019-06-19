package no.elg.infiniteBootleg.world.subgrid;

import no.elg.infiniteBootleg.world.World;
import org.jetbrains.annotations.NotNull;

public abstract class LivingEntity extends Entity {

    public static final int DEFAULT_HEALTH = 10;
    private int health;

    public LivingEntity(@NotNull World world, float x, float y) {
        super(world, x, y);
        health = DEFAULT_HEALTH;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }
}
