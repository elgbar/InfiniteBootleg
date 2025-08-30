package no.elg.infiniteBootleg.core.world

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.core.items.Item
import no.elg.infiniteBootleg.core.items.ItemType
import no.elg.infiniteBootleg.core.items.StaffItem
import no.elg.infiniteBootleg.core.world.magic.Equippable
import no.elg.infiniteBootleg.core.world.magic.Gem
import no.elg.infiniteBootleg.core.world.magic.MutableSpellState
import no.elg.infiniteBootleg.core.world.magic.Ring
import no.elg.infiniteBootleg.core.world.magic.SpellState
import no.elg.infiniteBootleg.core.world.magic.Wood
import no.elg.infiniteBootleg.protobuf.ElementKt.staff
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Element as ProtoElement

data class Staff(val wood: Wood, val gems: List<Gem>, val rings: List<Ring>) :
  Equippable,
  ContainerElement {

  init {
    check(gems.isNotEmpty()) { "A staff must have at least one gem" }
  }

  fun createSpellState(entity: Entity): MutableSpellState {
    val state = MutableSpellState(
      caster = entity,
      staff = this,
      // FIXME placeholder
      spellRange = 32.0,
      castDelay = wood.castDelay,
      gemPower = wood.rating.powerPercent,
      // FIXME placeholder
      spellVelocity = 10.0,
      entityModifications = mutableListOf()
    )
    wood.onSpellCreate(state)
    gems.forEach { it.onSpellCreate(state) }
    rings.forEach { it.onSpellCreate(state) }
    return state
  }

  fun onSpellCast(state: SpellState, spellEntity: Entity) {
    wood.onSpellCast(state, spellEntity)
    gems.forEach { it.onSpellCast(state, spellEntity) }
    rings.forEach { it.onSpellCast(state, spellEntity) }
  }

//  fun onSpellLand(state: SpellState, spellEntity: Entity, event: PhysicsEvent.ContactBeginsEvent) {
//    wood.onSpellLand(state, spellEntity)
//    gems.forEach { it.onSpellLand(state, spellEntity) }
//    rings.forEach { it.onSpellLand(state, spellEntity) }
//  }

  override fun onEquip(entity: Entity) {
    wood.onEquip(entity)
    gems.forEach { it.onEquip(entity) }
    rings.forEach { it.onEquip(entity) }
  }

  override fun onUnequip(entity: Entity) {
    wood.onUnequip(entity)
    gems.forEach { it.onUnequip(entity) }
    rings.forEach { it.onUnequip(entity) }
  }

  override val itemType: ItemType = ItemType.TOOL
  override fun toItem(maxStock: UInt, stock: UInt): Item = StaffItem(this, maxStock, stock)

  companion object {

    //    val textureRegion: RotatableTextureRegion get() = ClientMain.inst().assets.staffTexture
    fun ProtoElement.Staff.fromProto(): Staff =
      Staff(
        wood = Wood.fromProto(wood),
        gems = (listOf(primaryGem) + secondaryGemsList).mapNotNull(Gem.Companion::fromProto),
        rings = ringsList.mapNotNull(Ring.Companion::fromProto)
      )

    fun Staff.toProto(): ProtoElement.Staff =
      staff {
        wood = this@toProto.wood.toProto()
        val (firstGem, otherGems) = gems.firstOrNull()?.let { it to gems.drop(1) } ?: return@staff
        primaryGem = firstGem.toProto()
        secondaryGems += otherGems.map(Gem::toProto)
        rings += this@toProto.rings.map(Ring::toProto)
      }
  }
}
