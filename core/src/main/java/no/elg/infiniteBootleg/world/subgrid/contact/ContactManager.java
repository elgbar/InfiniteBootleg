package no.elg.infiniteBootleg.world.subgrid.contact;

import static no.elg.infiniteBootleg.world.GlobalLockKt.BOX2D_LOCK;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import no.elg.infiniteBootleg.world.box2d.CollisionListener;
import org.jetbrains.annotations.NotNull;

public class ContactManager implements ContactListener {

  private static void handleContact(@NotNull ContactType type, @NotNull Contact contact) {
    // Must be within a box2d lock to make sure the contact is valid
    synchronized (BOX2D_LOCK) {
      CollisionListener.INSTANCE.contact(type, contact);
      Body body = contact.getFixtureB().getBody();
      Object data = body.getUserData();
      if (data instanceof Entity) {
        CollisionListener.INSTANCE.contact(type, contact);
      }
      //      if (data instanceof Entity entity && !entity.isDisposed()) {
      //        entity.contact(type, contact);
      //      }
      //      if (data instanceof com.badlogic.ashley.core.Entity entity) {
      //        var engine = Main.inst().getEngine();
      //        if (engine == null) {
      //          Main.logger().warn("ContactManager", "Failed to get engine in handleContact");
      //          return;
      //        }
      //        CollisionListener.INSTANCE.contact(type, contact);
      //        handleAshleyContact(type, contact, engine, entity);
      //      }
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
