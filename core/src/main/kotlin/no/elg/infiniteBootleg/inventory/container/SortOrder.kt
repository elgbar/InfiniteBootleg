package no.elg.infiniteBootleg.inventory.container

import com.google.common.base.Preconditions
import no.elg.infiniteBootleg.inventory.container.impl.AutoSortedContainer
import no.elg.infiniteBootleg.items.Item
import java.util.Arrays

/**
 * The orders to sort an [AutoSortedContainer]
 *
 * @author kheba
 */
data class SortOrder(private val reverse: Boolean, val sorter: Comparator<Item?>) {

  /**
   * Sort the array with the given settings
   *
   * @param a The array to sort
   */
  fun sort(a: Array<Item?>) {
    Preconditions.checkNotNull(a)

    // finally sort the array using the compiled settings
    Arrays.sort(a, sorter)
  }

  companion object {
    private val TT_NAME: Comparator<Item> = Comparator.comparing { ts: Item -> ts.element.name }

    /** Sort descending by the name of the tile type with null at the end  */
    val ELEM_NAME_DESC: Comparator<Item?> = object : Comparator<Item?> {
      override fun compare(ts1: Item?, ts2: Item?): Int {
        val nullCmp = nullCmp(ts1, ts2)
        if (nullCmp != 0) {
          return nullCmp
        }
        return ts1!!.element.name.compareTo(ts2!!.element.name)
      }

      override fun reversed(): Comparator<Item?> {
        return TT_NAME_ASC
      }
    }

    /** Sort ascending by the name of the tile type with null at the end  */
    val TT_NAME_ASC: Comparator<Item?> = object : Comparator<Item?> {
      override fun compare(ts1: Item?, ts2: Item?): Int {
        val nullCmp = nullCmp(ts1, ts2)
        if (nullCmp != 0) {
          return nullCmp
        }
        return -ts1!!.element.name.compareTo(ts2!!.element.name)
      }

      override fun reversed(): Comparator<Item?> {
        return ELEM_NAME_DESC
      }
    }

    /** Sort by the amount of tiles in ascending order  */
    val AMOUNT_ASC: Comparator<Item?> = object : Comparator<Item?> {
      override fun compare(ts1: Item?, ts2: Item?): Int {
        val nullCmp = nullCmp(ts1, ts2)
        if (nullCmp != 0) {
          return nullCmp
        }
        return (ts1!!.stock - ts2!!.stock).toInt()
      }

      override fun reversed(): Comparator<Item?> {
        return AMOUNT_DESC
      }
    }

    /** Sort by the amount of tiles in descending order  */
    val AMOUNT_DESC: Comparator<Item?> = object : Comparator<Item?> {
      override fun compare(ts1: Item?, ts2: Item?): Int {
        val nullCmp = nullCmp(ts1, ts2)
        if (nullCmp != 0) {
          return nullCmp
        }
        return (ts2!!.stock - ts1!!.stock).toInt()
      }

      override fun reversed(): Comparator<Item?> {
        return AMOUNT_ASC
      }
    }

    private fun nullCmp(o1: Any?, o2: Any?): Int {
      return if (o1 == null) 1 else if (o2 == null) -1 else 0
    }

    /**
     * Sort the given array with the given settings. This is the same as declaring a new SortOrder
     * with the given settings and calling sort on `a`
     *
     * @param a The array to sort
     * @param reversed If the sort order should be reversed
     * @param sorts The comparators to sort the array
     */
    fun sort(a: Array<Item?>, reversed: Boolean, vararg sorts: Comparator<Item?>) {
      compileComparator(reversed, *sorts).sort(a)
    }

    /**
     * @param reverse If the sorts should be reversed
     * @param sorters The comparators used to sort `a`, the first will be the first to compare
     */
    fun compileComparator(reverse: Boolean, vararg sorters: Comparator<Item?>): SortOrder {
      Preconditions.checkArgument(sorters.isNotEmpty(), "There must be at least one way to sort")

      // create a  comparator that is using all the comparators
      val comparator = Comparator { ts1: Item?, ts2: Item? ->
        for (loopComp in sorters) {
          val result = loopComp.compare(ts1, ts2)

          // if using this comparator is equal, use the next comparator
          if (result == 0) {
            continue
          }
          return@Comparator result
        }
        0
      }

      if (reverse) {
        comparator.reversed()
      }
      return SortOrder(reverse, comparator)
    }
  }
}