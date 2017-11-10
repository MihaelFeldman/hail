package is.hail.variant

import is.hail.annotations._
import is.hail.expr._

import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

class StructViewSuite extends TestNGSuite {

  @Test
  def simpleStructs() {
    val region = MemoryBuffer()
    val rvb = new RegionValueBuilder(region)
    val source = TStruct("a" -> TInt32(), "b" -> TFloat64())
    rvb.start(source)
    rvb.startStruct()
    rvb.addInt(10)
    rvb.addDouble(42.0)
    rvb.endStruct()
    val off = rvb.end()

    {
      val view = new StructView(TStruct("b" -> TFloat64()), source)
      view.setRegion(region, off)
      assert(view.hasField("b"))
      assert(view.getDoubleField("b") == 42.0)
    }

    {
      val view = new StructView(TStruct("b" -> TInt32()), source)
      view.setRegion(region, off)
      assert(!view.hasField("b"))
    }

    {
      val view = new StructView(TStruct("a" -> TInt32()), source)
      view.setRegion(region, off)
      assert(view.hasField("a"))
      assert(view.getIntField("a") == 10)
    }

    {
      val view = new StructView(TStruct("a" -> TInt32(), "b" -> TFloat64()), source)
      view.setRegion(region, off)
      assert(view.hasField("a"))
      assert(view.getIntField("a") == 10)
      assert(view.hasField("b"))
      assert(view.getDoubleField("b") == 42.0)
    }

    {
      val view = new StructView(TStruct("b" -> TFloat64(), "a" -> TInt32()), source)
      view.setRegion(region, off)
      assert(view.hasField("a"))
      assert(view.getIntField("a") == 10)
      assert(view.hasField("b"))
      assert(view.getDoubleField("b") == 42.0)
    }

    {
      val view = new StructView(TStruct("b" -> TInt32(), "a" -> TInt32()), source)
      view.setRegion(region, off)
      assert(view.hasField("a"))
      assert(view.getIntField("a") == 10)
      assert(!view.hasField("b"))
    }

    {
      val view = new StructView(TStruct("b" -> TInt32(), "a" -> TFloat64()), source)
      view.setRegion(region, off)
      assert(!view.hasField("a"))
      assert(!view.hasField("b"))
    }

    {
      val view = new StructView(TStruct("c" -> TBoolean(), "b" -> TFloat64(), "d" -> TInt32(), "a" -> TInt32()), source)
      view.setRegion(region, off)
      assert(view.hasField("a"))
      assert(view.getIntField("a") == 10)
      assert(view.hasField("b"))
      assert(view.getDoubleField("b") == 42.0)
      assert(!view.hasField("c"))
      assert(!view.hasField("d"))
    }
  }
}
