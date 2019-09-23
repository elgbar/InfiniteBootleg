package no.elg.infiniteBootleg.world.subgrid.enitites;

import com.badlogic.gdx.math.Vector2;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import org.jetbrains.annotations.NotNull;

public abstract class AirResistanceEntity extends Entity {

    /*
        p is density of fluid
        v is speed of object relative to the fluid
        a is the cross-section area
        cd is the drag coefficient - a dimensionless number
            cubes = 1.05
            sphere = 0.47
            streamlined-body (smooth wing shape) = 0.04
     */
    public static final float FLUID_DENSITY_OF_AIR = 1f;
    public static final float DRAG_COEFFICIENT_CUBE = 1.05f;
    public static final float DRAG_COEFFICIENT_SPHERE = 0.47f;
    public static final float DRAG_COEFFICIENT_WING = 0.04f;

    public static final float MIN_SPEED = 0.75f;

    public AirResistanceEntity(@NotNull World world, float worldX, float worldY) {
        super(world, worldX, worldY);
    }

    private float getSpeed() {
        return getBody().getLinearVelocity().len();
    }

    @Override
    public void update() {
        super.update();


        float speed = getSpeed();

        float a = 1.0f;
        float cd = DRAG_COEFFICIENT_CUBE;
        float v = (float) Math.pow(speed, 2);
        float dragForce = 0.5f * FLUID_DENSITY_OF_AIR * v * cd * a;
        float dragAngle = getBody().getLinearVelocity().angle();
        Vector2 appliedDrag = new Vector2(dragForce, 0);
        appliedDrag.setAngle(dragAngle);
        appliedDrag.scl(-1);

        if (!isFlying()) {
            appliedDrag.y = 0;
        }
        if (Math.abs(dragForce) < MIN_SPEED) {
            return;
        }
//        System.out.println("appliedDrag = " + appliedDrag);
        getBody().applyForceToCenter(appliedDrag, true);

    }
}
