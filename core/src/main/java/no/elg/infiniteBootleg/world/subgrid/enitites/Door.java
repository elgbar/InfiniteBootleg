package no.elg.infiniteBootleg.world.subgrid.enitites;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import java.util.concurrent.atomic.AtomicInteger;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import no.elg.infiniteBootleg.world.Direction;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import static no.elg.infiniteBootleg.world.World.BLOCK_ENTITY_FILTER;
import static no.elg.infiniteBootleg.world.World.TRANSPARENT_BLOCK_ENTITY_FILTER;
import no.elg.infiniteBootleg.world.subgrid.MaterialEntity;
import no.elg.infiniteBootleg.world.subgrid.contact.ContactType;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class Door extends MaterialEntity {

    public static final String OPEN_DOOR_REGION_NAME = "door_open";
    public static final String CLOSED_DOOR_REGION_NAME = "door_closed";

    private TextureRegion openDoorRegion;
    private TextureRegion closedDoorRegion;

    private final AtomicInteger open = new AtomicInteger();

    public Door(@NotNull World world, float worldX, float worldY) {
        super(world, worldX, worldY);
        if (Settings.renderGraphic) {
            openDoorRegion = Main.inst().getEntityAtlas().findRegion(OPEN_DOOR_REGION_NAME);
            closedDoorRegion = Main.inst().getEntityAtlas().findRegion(CLOSED_DOOR_REGION_NAME);
        }
    }

    @Override
    public Material getMaterial() {
        return Material.DOOR;
    }

    @NotNull
    @Override
    protected BodyDef createBodyDef(float worldX, float worldY) {
        BodyDef def = super.createBodyDef(worldX, worldY);
        def.type = BodyDef.BodyType.StaticBody;
        return def;
    }

    @Override
    protected void createFixture(@NotNull Body body) {
        PolygonShape box = new PolygonShape();
        box.setAsBox(getHalfBox2dWidth(), getHalfBox2dHeight());
        Fixture fix = body.createFixture(box, 0.0f);
        fix.setFilterData(BLOCK_ENTITY_FILTER);
        fix.setSensor(true);
        box.dispose();
    }

    @Override
    public void contact(@NotNull ContactType type, @NotNull Contact contact) {
        if (type == ContactType.BEGIN_CONTACT) {
            var old = open.getAndIncrement();
            if (old == 0) {
                setFilter(TRANSPARENT_BLOCK_ENTITY_FILTER);
            }
        }
        else if (type == ContactType.END_CONTACT) {
            var old = open.decrementAndGet();
            if (old == 0) {
                setFilter(BLOCK_ENTITY_FILTER);
            }
        }

    }

    @Override
    public boolean isOnGround() {
        //it's on the ground if the block below is not air
        return !getWorld().isAirBlock(Location.relative(getBlockX(), getBlockY(), Direction.SOUTH));
    }

    @Override
    public TextureRegion getTextureRegion() {
        return open.get() == 0 ? openDoorRegion : closedDoorRegion;
    }

    @Override
    public int getWidth() {
        return 2 * BLOCK_SIZE;
    }

    @Override
    public int getHeight() {
        return 4 * BLOCK_SIZE;
    }
}
