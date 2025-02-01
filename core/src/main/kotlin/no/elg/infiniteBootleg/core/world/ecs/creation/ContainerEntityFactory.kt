package no.elg.infiniteBootleg.core.world.ecs.creation

import com.badlogic.ashley.core.Engine
import ktx.ashley.allOf
import no.elg.infiniteBootleg.core.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.core.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.core.inventory.container.impl.ContainerImpl
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.ecs.BASIC_BLOCK_ENTITY
import no.elg.infiniteBootleg.core.world.ecs.components.NameComponent.Companion.nameOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent
import no.elg.infiniteBootleg.core.world.ecs.components.tags.AuthoritativeOnlyTag.Companion.authoritativeOnly
import no.elg.infiniteBootleg.core.world.ecs.components.tags.CanBeOutOfBoundsTag.Companion.canBeOutOfBounds
import no.elg.infiniteBootleg.core.world.world.World

fun Engine.createContainerEntity(world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material) =
  createBlockEntity(world, worldX, worldY, material, arrayOf(allOf(*BASIC_BLOCK_ENTITY, ContainerComponent::class).get() to "container block")) {
    entity.canBeOutOfBounds = true // containers can be out of bounds, as they are removed by chunks when it is unloaded
    entity.authoritativeOnly = true
    entity.safeWith {
      val container = ContainerImpl(entity.nameOrNull ?: "Container")
      val owner = ContainerOwner.Companion.from(worldX, worldY)
      ContainerComponent(OwnedContainer(owner, container))
    }
  }
