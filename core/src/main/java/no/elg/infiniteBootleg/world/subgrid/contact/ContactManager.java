package no.elg.infiniteBootleg.world.subgrid.contact;

import static no.elg.infiniteBootleg.world.GlobalLockKt.BOX2D_LOCK;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.world.ecs.OnGroundComp;
import no.elg.infiniteBootleg.world.ecs.PositionComp;
import no.elg.infiniteBootleg.world.subgrid.Entity;
import org.jetbrains.annotations.NotNull;

public class ContactManager implements ContactListener {

  private static void handleContact(@NotNull ContactType type, @NotNull Contact contact) {
    // Must be within a box2d lock to make sure the contact is valid
    synchronized (BOX2D_LOCK) {
      Body body = contact.getFixtureB().getBody();
      Object data = body.getUserData();
      if (data instanceof Entity entity && !entity.isDisposed()) {
        entity.contact(type, contact);
      }
      if (data instanceof com.badlogic.ashley.core.Entity entity) {
        var engine = Main.inst().getEngine();
        if (engine == null) {
          Main.logger().warn("ContactManager", "Failed to get engine in handleContact");
          return;
        }
        handleAshleyContact(type, contact, engine, entity);
      }
    }
  }

  private static void handleAshleyContact(
      @NotNull ContactType type,
      @NotNull Contact contact,
      @NotNull Engine engine,
      @NotNull com.badlogic.ashley.core.Entity entity) {
    var onGroundComp = OnGroundComp.Companion.getMapper().get(entity);
    var positionComp = PositionComp.Companion.getMapper().get(entity);
    if (onGroundComp != null) {
      if (type == ContactType.BEGIN_CONTACT) {
        // newest pos is needed to accurately check if this is on ground
        updatePos();

        int y = MathUtils.floor(posCache.y - getHalfBox2dHeight() - GROUND_CHECK_OFFSET);

        int leftX = MathUtils.ceil(posCache.x - (2 * getHalfBox2dWidth()));
        int middleX = MathUtils.floor(posCache.x - getHalfBox2dWidth());
        int rightX = MathUtils.ceil(posCache.x - GROUND_CHECK_OFFSET);

        int detected = 0;

        if (!world.isAirBlock(leftX, y)) {
          detected++;
        }
        if (!world.isAirBlock(middleX, y)) {
          detected++;
        }
        if (!world.isAirBlock(rightX, y)) {
          detected++;
        }
        if (detected > 0) {
          groundContacts++;
        }
      } else if (type == ContactType.END_CONTACT) {
        groundContacts--;
        if (groundContacts < 0) {
          groundContacts = 0;
        }
      }
    }
  }

  @Override
  public void beginContact(Contact contact) {
    handleContact(ContactType.BEGIN_CONTACT, contact);
  }

  @Override
  public void endContact(Contact contact) {
    handleContact(ContactType.END_CONTACT, contact);
  }

  @Override
  public void preSolve(Contact contact, Manifold oldManifold) {}

  @Override
  public void postSolve(Contact contact, ContactImpulse impulse) {}
}
