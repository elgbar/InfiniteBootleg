package no.elg.infiniteBootleg.world.magic

import com.badlogic.ashley.core.Entity
import no.elg.infiniteBootleg.protobuf.EntityKt.StaffKt.gem
import no.elg.infiniteBootleg.protobuf.EntityKt.StaffKt.ring
import no.elg.infiniteBootleg.protobuf.EntityKt.StaffKt.wood
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.magic.parts.GemRating
import no.elg.infiniteBootleg.world.magic.parts.GemType
import no.elg.infiniteBootleg.world.magic.parts.RingRating
import no.elg.infiniteBootleg.world.magic.parts.RingType
import no.elg.infiniteBootleg.world.magic.parts.WoodRating
import no.elg.infiniteBootleg.world.magic.parts.WoodType

data class Wood(val type: WoodType, val rating: WoodRating) : MagicEffects, Equippable {

  val castDelay by lazy { type.castDelay / rating.powerPercent }

  override fun onSpellCreate(state: MutableSpellState) = type.onSpellCreate(state, rating)
  override fun onSpellCast(state: SpellState, spellEntity: Entity) = type.onSpellCast(state, spellEntity, rating)
  override fun onSpellLand(state: SpellState, spellEntity: Entity) = type.onSpellLand(state, spellEntity, rating)

  fun toProto(): ProtoWorld.Entity.Staff.Wood =
    wood {
      this.type = this@Wood.type.displayName
      this.rating = this@Wood.rating.name
    }

  companion object {
    fun fromProto(proto: ProtoWorld.Entity.Staff.Wood): Wood {
      return Wood(WoodType.valueOf(proto.type), WoodRating.valueOf(proto.rating))
    }
  }
}

data class Gem(val type: GemType, val rating: GemRating) : MagicEffects, Equippable {

  val power = rating.powerPercent

  override fun onSpellCreate(state: MutableSpellState) = type.onSpellCreate(state, rating)
  override fun onSpellCast(state: SpellState, spellEntity: Entity) = type.onSpellCast(state, spellEntity, rating)
  override fun onSpellLand(state: SpellState, spellEntity: Entity) = type.onSpellLand(state, spellEntity, rating)

  fun toProto(): ProtoWorld.Entity.Staff.Gem =
    gem {
      this.type = this@Gem.type.displayName
      this.rating = this@Gem.rating.name
    }

  companion object {
    fun fromProto(proto: ProtoWorld.Entity.Staff.Gem): Gem {
      return Gem(GemType.valueOf(proto.type), GemRating.valueOf(proto.rating))
    }
  }
}

data class Ring(val type: RingType<RingRating?>, val rating: RingRating?) : MagicEffects, Equippable {

  override fun onSpellCreate(state: MutableSpellState) = type.onSpellCreate(state, rating)
  override fun onSpellCast(state: SpellState, spellEntity: Entity) = type.onSpellCast(state, spellEntity, rating)
  override fun onSpellLand(state: SpellState, spellEntity: Entity) = type.onSpellLand(state, spellEntity, rating)

  fun toProto(): ProtoWorld.Entity.Staff.Ring =
    ring {
      this.type = this@Ring.type.serializedName
      this@Ring.rating?.let { this.rating = it.name }
    }

  companion object {
    fun fromProto(proto: ProtoWorld.Entity.Staff.Ring): Ring {
      return Ring(
        RingType.valueOf(proto.type),
        if (proto.hasRating()) RingRating.valueOf(proto.rating) else null
      )
    }
  }
}
