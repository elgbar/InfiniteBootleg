package no.elg.infiniteBootleg.items

import no.elg.infiniteBootleg.world.Material

data class Item(val material: Material, val charge: UInt, val maxCharge: UInt = UInt.MAX_VALUE) {

  fun use(charges: UInt = 1u): Item {
    return Item(material, charge - charges, maxCharge)
  }
}
