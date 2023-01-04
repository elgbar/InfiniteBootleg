package no.elg.infiniteBootleg.world.box2d

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.Fixture
import no.elg.infiniteBootleg.world.Constants
import no.elg.infiniteBootleg.world.ecs.components.GroundedComponent.Companion.groundedOrNull
import no.elg.infiniteBootleg.world.ecs.components.required.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.position
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.subgrid.contact.ContactHandler
import no.elg.infiniteBootleg.world.subgrid.contact.ContactType

object CollisionListener : ContactHandler {

  /**
   * Must be called while synchronized under BOX2D_LOCK
   *
   * @param type    The type of contact
   * @param contact Contact made
   */
  override fun contact(type: ContactType, contact: Contact) {
    if (type == ContactType.BEGIN_CONTACT) {
      if (contact.fixtureA.filterData.categoryBits == Filters.GROUND_CATEGORY) {
        a(contact.fixtureB, type)
      } else if (contact.fixtureB.filterData.categoryBits == Filters.GROUND_CATEGORY) {
        a(contact.fixtureA, type)
      }
    }
  }

  private fun a(entityFixture: Fixture, type: ContactType) {
    val entity = entityFixture.userData as Entity? ?: return
    // newest pos is needed to accurately check if this is on ground
//      updatePos()
    val grounded = entity.groundedOrNull ?: return

    if (type == ContactType.BEGIN_CONTACT) {
      val posCache = entity.position
      val box2d = entity.box2d
      val world = entity.world.world

      val y = MathUtils.floor(posCache.y - box2d.halfBox2dHeight - Constants.GROUND_CHECK_OFFSET)
      val leftX = MathUtils.ceil(posCache.x - 2 * box2d.halfBox2dWidth)
      val middleX = MathUtils.floor(posCache.x - box2d.halfBox2dWidth)
      val rightX = MathUtils.ceil(posCache.x - Constants.GROUND_CHECK_OFFSET)
      var detected = 0
      if (!world.isAirBlock(leftX, y)) {
        detected++
      }
      if (!world.isAirBlock(middleX, y)) {
        detected++
      }
      if (!world.isAirBlock(rightX, y)) {
        detected++
      }
      if (detected > 0) {
        grounded += 1
      }
    } else if (type == ContactType.END_CONTACT) {
      grounded -= 1
    }
  }
}
