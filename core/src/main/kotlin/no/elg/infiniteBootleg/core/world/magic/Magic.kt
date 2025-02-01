package no.elg.infiniteBootleg.core.world.magic

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.core.world.magic.parts.GemRating
import no.elg.infiniteBootleg.core.world.magic.parts.GemType
import no.elg.infiniteBootleg.core.world.magic.parts.RingRating
import no.elg.infiniteBootleg.core.world.magic.parts.RingType
import no.elg.infiniteBootleg.core.world.magic.parts.WoodRating
import no.elg.infiniteBootleg.core.world.magic.parts.WoodType
import no.elg.infiniteBootleg.protobuf.ElementKt.StaffKt.gem
import no.elg.infiniteBootleg.protobuf.ElementKt.StaffKt.ring
import no.elg.infiniteBootleg.protobuf.ElementKt.StaffKt.wood
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Element as ProtoElement

data class Wood(val type: WoodType, val rating: WoodRating) : MagicEffects, Equippable {

  val castDelay by lazy { type.castDelay / rating.powerPercent }

  override fun onSpellCreate(state: MutableSpellState) = type.onSpellCreate(state, rating)
  override fun onSpellCast(state: SpellState, spellEntity: Entity) = type.onSpellCast(state, spellEntity, rating)
  override fun onSpellLand(state: SpellState, spellEntity: Entity) = type.onSpellLand(state, spellEntity, rating)

  fun toProto(): ProtoElement.Staff.Wood =
    wood {
      this.type = this@Wood.type.displayName
      this.rating = this@Wood.rating.name
    }

  companion object {
    fun fromProto(proto: ProtoElement.Staff.Wood): Wood {
      return Wood(WoodType.Companion.valueOf(proto.type), WoodRating.valueOf(proto.rating))
    }
  }
}

data class Gem(val type: GemType, val rating: GemRating) : MagicEffects, Equippable {

  val power = rating.powerPercent

  override fun onSpellCreate(state: MutableSpellState) = type.onSpellCreate(state, rating)
  override fun onSpellCast(state: SpellState, spellEntity: Entity) = type.onSpellCast(state, spellEntity, rating)
  override fun onSpellLand(state: SpellState, spellEntity: Entity) = type.onSpellLand(state, spellEntity, rating)

  fun toProto(): ProtoElement.Staff.Gem =
    gem {
      this.type = this@Gem.type.displayName
      this.rating = this@Gem.rating.name
    }

  companion object {
    fun fromProto(proto: ProtoElement.Staff.Gem): Gem? = GemType.Companion.valueOf(proto.type)?.let { gemType -> Gem(gemType, GemRating.valueOf(proto.rating)) }
  }
}

data class Ring(val type: RingType<RingRating?>, val rating: RingRating?) : MagicEffects, Equippable {

  override fun onSpellCreate(state: MutableSpellState) = type.onSpellCreate(state, rating)
  override fun onSpellCast(state: SpellState, spellEntity: Entity) = type.onSpellCast(state, spellEntity, rating)
  override fun onSpellLand(state: SpellState, spellEntity: Entity) = type.onSpellLand(state, spellEntity, rating)

  fun toProto(): ProtoElement.Staff.Ring =
    ring {
      this.type = this@Ring.type.serializedName
      this@Ring.rating?.let { this.rating = it.name }
    }

  companion object {
    fun fromProto(proto: ProtoElement.Staff.Ring): Ring? =
      RingType.Companion.valueOf(proto.type)?.let { ringType ->
        Ring(
          ringType,
          if (proto.hasRating()) RingRating.valueOf(proto.rating) else null
        )
      }
  }
}
