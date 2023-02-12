package no.elg.infiniteBootleg.world.subgrid.enitites;

import static no.elg.infiniteBootleg.world.Block.BLOCK_SIZE;
import static no.elg.infiniteBootleg.world.box2d.Filters.EN_GR_LI__GROUND_FILTER;
import static no.elg.infiniteBootleg.world.box2d.Filters.EN_GR__GROUND_FILTER;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.concurrent.GuardedBy;
import no.elg.infiniteBootleg.KAssets;
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
@Deprecated
public class Door extends MaterialEntity {

  public static final String OPEN_DOOR_REGION_NAME = "door_open";
  public static final String CLOSED_DOOR_REGION_NAME = "door_closed";

  private static final TextureRegion openDoorRegion;
  private static final TextureRegion closedDoorRegion;

  private final AtomicInteger contacts = new AtomicInteger();

  private final Location southBlock = Location.relative(getBlockX(), getBlockY(), Direction.SOUTH);

  public Door(@NotNull World world, @NotNull ProtoWorld.Entity protoEntity) {
    super(world, protoEntity);
  }

  public Door(@NotNull World world, float worldX, float worldY) {
    super(world, worldX, worldY);
  }

  static {
    if (Settings.client) {
      openDoorRegion = KAssets.INSTANCE.getEntityAtlas().findRegion(OPEN_DOOR_REGION_NAME);
      closedDoorRegion = KAssets.INSTANCE.getEntityAtlas().findRegion(CLOSED_DOOR_REGION_NAME);
    } else {
      openDoorRegion = null;
      closedDoorRegion = null;
    }
  }

  {
    if (!isDisposed()) {

      postWorldBodyRunnable(
          body -> {
            setFilter(EN_GR_LI__GROUND_FILTER);

            // Wake up all bodies to get an accurate contacts count
            final Vector2 position = body.getPosition();
            final WorldBody worldBody = getWorld().getWorldBody();
            worldBody.queryAABB(
                position.x,
                position.y,
                position.x + getHalfBox2dWidth() * 2,
                position.y + getHalfBox2dHeight() * 2,
                fixture -> {
                  Body fixtBody = fixture.getBody();
                  if (fixtBody != null && fixtBody.getUserData() != this) {
                    fixtBody.setAwake(true);
                  }
                  return true;
                });
            return null;
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
  @GuardedBy("no.elg.infiniteBootleg.world.GlobalLockKt.BOX2D_LOCK")
  protected void createFixture(@NotNull Body body) {
    PolygonShape box = new PolygonShape();
    box.setAsBox(getHalfBox2dWidth(), getHalfBox2dHeight());
    Fixture fix = body.createFixture(box, 0.0f);
    fix.setFilterData(EN_GR_LI__GROUND_FILTER);
    fix.setSensor(true);
    box.dispose();
  }

  @Override
  public void contact(@NotNull ContactType type, @NotNull Contact contact) {
    if (type == ContactType.BEGIN_CONTACT) {
      int oldContacts = contacts.getAndIncrement();
      if (oldContacts == 0) {
        setFilter(EN_GR__GROUND_FILTER);
      }
    } else if (type == ContactType.END_CONTACT) {
      int newContacts = contacts.decrementAndGet();
      if (newContacts <= 0) {
        setFilter(EN_GR_LI__GROUND_FILTER);
      }
    }
    final int cont = contacts.get();
    if (cont < 0) {
      Main.logger().error("DOOR", "Negative contacts! (" + cont + ")");
      contacts.set(0);
    }
  }

  @Override
  public boolean isOnGround() {
    // it's on the ground if the block below is not air
    return !getWorld().isAirBlock(southBlock);
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
