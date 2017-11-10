package is.hail.variant

import is.hail.annotations._
import is.hail.expr._
import is.hail.utils._

class ArrayView(element: Type, view: View) {
  private val t = TArray(element)
  private var region: MemoryBuffer = _
  private var aoff: Long = _
  private var length: Int = _
  private var off: Long = _
  private var defined: Boolean = _

  def setRegion(rv: RegionValue) {
    setRegion(rv.region, rv.offset)
  }

  def setRegion(region: MemoryBuffer, offset: Long) {
    this.region = region
    this.aoff = offset
    this.length = TContainer.loadLength(region, aoff)
  }

  def set(i: Int) {
    require(i >= 0 && i < length)
    defined = t.isElementDefined(region, aoff, i)
    off = t.loadElement(region, aoff, length, i)
    view.setRegion(region, off)
  }
  def has(): Boolean = defined
}
