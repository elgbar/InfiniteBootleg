package no.elg.infiniteBootleg.world.subgrid.enitites;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.World.BLOCK_ENTITY_FILTER;
import static no.elg.infiniteBootleg.world.World.TRANSPARENT_BLOCK_ENTITY_FILTER;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import java.util.concurrent.atomic.AtomicInteger;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import no.elg.infiniteBootleg.world.Direction;
import no.elg.infiniteBootleg.world.Location;
import no.elg.infiniteBootleg.world.Material;
import no.elg.infiniteBootleg.world.World;
import no.elg.infiniteBootleg.world.box2d.WorldBody;
import no.elg.infiniteBootleg.world.subgrid.MaterialEntity;
import no.elg.infiniteBootleg.world.subgrid.contact.ContactType;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class Door extends MaterialEntity {

    public static final String OPEN_DOOR_REGION_NAME = "door_open";
    public static final String CLOSED_DOOR_REGION_NAME = "door_closed";

    private static final TextureRegion openDoorRegion;
    private static final TextureRegion closedDoorRegion;

    private final AtomicInteger contacts = new AtomicInteger();

    public Door(@NotNull World world, ProtoWorld.@NotNull Entity protoEntity) {
        super(world, protoEntity);
    }

    public Door(@NotNull World world, float worldX, float worldY) {
        super(world, worldX, worldY);
    }

    static {
        if (Settings.client) {
            openDoorRegion = Main.inst().getEntityAtlas().findRegion(OPEN_DOOR_REGION_NAME);
            closedDoorRegion = Main.inst().getEntityAtlas().findRegion(CLOSED_DOOR_REGION_NAME);
        }
        else {
            openDoorRegion = null;
            closedDoorRegion = null;
        }
    }

    {
        if (!isInvalid()) {
            //Wake up all bodies to get an accurate contacts count
            final Vector2 position = getBody().getPosition();
            final WorldBody worldBody = getWorld().getWorldBody();
            worldBody.queryAABB(position.x, position.y, position.x + getHalfBox2dWidth() * 2, position.y + getHalfBox2dHeight() * 2, fixture -> {
                final Body body = fixture.getBody();
                if (body != null && body.getUserData() != this) {
                    body.setAwake(true);
                }
                return true;
            });
        }
    }

    @NotNull
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
            int oldContacts = contacts.getAndIncrement();
            if (oldContacts == 0) {
                setFilter(TRANSPARENT_BLOCK_ENTITY_FILTER);
                Main.inst().getScheduler().executeAsync(() -> getWorld().updateLights());
            }
        }
        else if (type == ContactType.END_CONTACT) {
            int newContacts = contacts.decrementAndGet();
            if (newContacts == 0) {
                setFilter(BLOCK_ENTITY_FILTER);
                Main.inst().getScheduler().executeAsync(() -> getWorld().updateLights());
            }
        }
        final int cont = contacts.get();
        if (cont < 0) {
            Main.inst().getConsoleLogger().error("DOOR", "Negative contacts! (" + cont + ")");
            contacts.set(0);
        }
    }

    @Override
    public boolean isOnGround() {
        //it's on the ground if the block below is not air
        return !getWorld().isAirBlock(Location.relative(getBlockX(), getBlockY(), Direction.SOUTH));
    }

    @Override
    public TextureRegion getTextureRegion() {
        return contacts.get() == 0 ? closedDoorRegion : openDoorRegion;
    }

    @Override
    public @NotNull String hudDebug() {
        return "contacts: " + contacts;
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
