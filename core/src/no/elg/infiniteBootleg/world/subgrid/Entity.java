package no.elg.infiniteBootleg.world.subgrid;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import no.elg.infiniteBootleg.world.Direction;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.render.Updatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * An entity that can move between the main world grid. The position of each entity is recorded in world coordinates.
 */
public abstract class Entity implements Updatable {

    public static final float DEFAULT_GRAVITY = -5f;
    public static final float DEFAULT_DRAG = 10f;
    public static final float DEFAULT_JUMP_FORCE = 2f;
    public static final float DEFAULT_ACCELERATION = 5f;
    private static final float VELOCITY_CUTOFF = 0.001f;
    private float gravity;
    private float drag;
    private float jumpForce;
    private float acceleration;

    //    private Bresenham2 bresenham;
    private World world;
    private Vector2 position;
    private Vector2 velocity;
    private boolean flying;

    private UUID uuid;


    public Entity(@NotNull World world, float x, float y) {
        this(world, new Vector2(x, y));
    }

    private Entity(@NotNull World world, @NotNull Vector2 position) {
        uuid = UUID.randomUUID();
        world.getEntities().add(this);
        this.world = world;
        this.position = position;
        velocity = new Vector2(0, 0);
        gravity = DEFAULT_GRAVITY;
        drag = DEFAULT_DRAG;
        jumpForce = DEFAULT_JUMP_FORCE;
        acceleration = DEFAULT_ACCELERATION;
//        bresenham = new Bresenham2();
        flying = false;
    }

    /**
     * @return The texture of this entity
     */
    public abstract TextureRegion getTextureRegion();

    /**
     * @return The width of this entity
     */
    public abstract float getWidth();

    /**
     * @return The height of this entity
     */
    public abstract float getHeight();

    /**
     * @param dx
     *     The x component of a relative point
     * @param dy
     *     The y component of a relative point
     *
     * @return The exact collision point
     */
    @Nullable
    public Direction collide(float dx, float dy) {
//        System.out.printf("ox:%d|oy:%d|nx:%d|ny:%d", (int) Math.floor(position.x), (int) Math.floor(position.y),
//                          (int) Math.floor(position.x + dx), (int) Math.floor(position.y + dy));
//        Array<GridPoint2> grid = bresenham
//            .line((int) Math.floor(position.x), (int) Math.floor(position.y), (int) Math.floor(position.x + dx),
//                  (int) Math.floor(position.y + dy));
//        System.out.println("grid = " + grid);
//        for (GridPoint2 point : new Array.ArrayIterable<>(grid)) {
        if (world.willCollide(position.x + dx, position.y + dy, getWidth(), getHeight())) {
            float angle = new Vector2(dx, dy).nor().angle();
            return Direction.fromAngle(angle);
        }
        return null;
    }

    @Override
    public void update() {
        float delta = Gdx.graphics.getDeltaTime();
        if (!velocity.isZero()) {
            float dx = velocity.x * delta;
            float dy = velocity.y * delta;
            Direction direction = collide(dx, dy);
            if (direction != null) {
                float sclX = direction.dx == 0 ? 1 : 0;
                float sclY = direction.dy == 0 ? 1 : 0;
                //there must be a collision point between
                velocity.scl(sclX, sclY);
            }
            else {
                position.add(dx, dy);
            }
        }
        if (!flying) {
            velocity.add(0, gravity);
        }
        float dvx = -velocity.x * drag * delta;
        float dvy = -velocity.y * drag * delta;
//        System.out.printf("dt:%.3f|d:%f|dv:(%.4f,%.4f) | v:%s%n", delta, drag, dvx, dvy, velocity);
        velocity.add(dvx, dvy);
        if (Math.abs(velocity.x) < VELOCITY_CUTOFF) {
            velocity.x = 0;
        }
        if (Math.abs(velocity.y) < VELOCITY_CUTOFF) {
            velocity.y = 0;
        }
    }


    public Vector2 getPosition() {
        return position;
    }

    public Vector2 getVelocity() {
        return velocity;
    }

    public boolean isFlying() {
        return flying;
    }

    public void setFlying(boolean flying) {
        this.flying = flying;
    }

    public float getGravity() {
        return gravity;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public float getDrag() {
        return drag;
    }

    public void setDrag(float drag) {
        this.drag = drag;
    }

    public float getJumpForce() {
        return jumpForce;
    }

    public void setJumpForce(float jumpForce) {
        this.jumpForce = jumpForce;
    }

    public World getWorld() {
        return world;
    }

    public UUID getUuid() {
        return uuid;
    }

    public float getAcceleration() {
        return acceleration;
    }

    public void setAcceleration(float acceleration) {
        this.acceleration = acceleration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        Entity entity = (Entity) o;
        return uuid.equals(entity.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
