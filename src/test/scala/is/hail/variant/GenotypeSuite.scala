package is.hail.variant

import is.hail.check.Prop._
import is.hail.check.{Gen, Properties}
import is.hail.utils.{ByteIterator, _}
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.collection.mutable

object GenotypeSuite {
  object Spec extends Properties("Genotype") {

    property("gt") = forAll { g: Genotype =>
      Genotype.gt(g).isDefined == Genotype.isCalled(g)
    }

    property("gtPairIndex") = forAll(Gen.choose(0, 0x7fff),
      Gen.choose(0, 0x7fff)) { (i: Int, j: Int) =>
      (i <= j) ==> (Genotype.gtPair(Genotype.gtIndex(i, j)) == GTPair(i, j))
    }

    property("gtIndexPair") = forAll(Gen.choose(0, 0x20003fff)) { (i: Int) =>
      val p = Genotype.gtPair(i)

      Genotype.gtIndex(p) == i &&
        Genotype.gtPairSqrt(i) == p &&
        Genotype.gtPairRecursive(i) == p
    }
  }

}

class GenotypeSuite extends TestNGSuite {

  import GenotypeSuite._

  val v = Variant("1", 1, "A", "T")

  @Test def testGenotype() {
    intercept[IllegalArgumentException] {
      Genotype(Some(-2), Some(Array(2, 0)), Some(2), None)
    }

    val noCall = Genotype(None, Some(Array(2, 0)), Some(2), None)
    val homRef = Genotype(Some(0), Some(Array(10, 0)), Some(10), Some(99), Some(Array(0, 1000, 100)))
    val het = Genotype(Some(1), Some(Array(5, 5)), Some(12), Some(99), Some(Array(100, 0, 1000)))
    val homVar = Genotype(Some(2), Some(Array(2, 10)), Some(12), Some(99), Some(Array(100, 1000, 0)))

    assert(!Genotype.isCalled(noCall) && !Genotype.isHomRef(noCall) && !Genotype.isHet(noCall) && !Genotype.isHomVar(noCall))
    assert(Genotype.isCalled(homRef) && Genotype.isHomRef(homRef) && !Genotype.isHet(homRef) && !Genotype.isHomVar(homRef))
    assert(Genotype.isCalled(het) && !Genotype.isHomRef(het) && Genotype.isHet(het) && !Genotype.isHomVar(het))
    assert(Genotype.isCalled(homVar) && !Genotype.isHomRef(homVar) && !Genotype.isHet(homVar) && Genotype.isHomVar(homVar))

    assert(Genotype.gt(noCall).isEmpty)
    assert(Genotype.gt(homRef).isDefined)
    assert(Genotype.gt(het).isDefined)
    assert(Genotype.gt(homVar).isDefined)

    assert(Genotype.pAB(Genotype(None, None, None, None)).isEmpty)
    assert(Genotype.pAB(Genotype(None, Some(Array(0, 0)), Some(0), None, None)).isEmpty)
    assert(D_==(Genotype.pAB(Genotype(Some(1), Some(Array(16, 16)), Some(33), Some(99), Some(Array(100, 0, 100)))).get, 1.0))
    assert(D_==(Genotype.pAB(Genotype(Some(4), Some(Array(16, 16, 16)), Some(48), None, None)).get, 1.0))
    assert(D_==(Genotype.pAB(Genotype(Some(4), Some(Array(16, 5, 8)), Some(48), None, None)).get, 0.423950))
    assert(D_==(Genotype.pAB(Genotype(Some(1), Some(Array(5, 8)), Some(13), Some(99), Some(Array(200, 0, 100)))).get, 0.423950))

    Spec.check()
  }

  @Test def gtPairGtIndexIsId() {
    forAll(Gen.choose(0, 32768), Gen.choose(0, 32768)) { (x, y) =>
      val (j, k) = if (x < y) (x, y) else (y, x)
      val gt = GTPair(j, k)
      Genotype.gtPair(Genotype.gtIndex(gt)) == gt
    }.check()
  }

  def triangleNumberOf(i: Int) = (i * i + i) / 2

  @Test def gtIndexGtPairIsId() {
    forAll(Gen.choose(0, 10000)) { (idx) =>
      Genotype.gtIndex(Genotype.gtPair(idx)) == idx
    }.check()
  }

  @Test def gtPairAndGtPairSqrtEqual() {
    forAll(Gen.choose(0, 10000)) { (idx) =>
      Genotype.gtPair(idx) == Genotype.gtPairSqrt(idx)
    }.check()
  }

  @Test def testGtFromLinear() {
    val gen = for (nGenotype <- Gen.choose(2, 5).map(triangleNumberOf);
      dosageGen = Gen.partition(nGenotype, 32768);
      result <- dosageGen) yield result

    val p = forAll(gen) { gp =>
      val gt = Genotype.gtFromLinear(gp)
      assert(gp.sum == 32768)
      val dMax = gp.max

      val check1 = gt.forall { gt =>
        val dosageP = gp(gt)
        dosageP == dMax && gp.zipWithIndex.forall { case (d, index) => index == gt || d != dosageP }
      }

      val check2 = gp.count(_ == dMax) > 1 || gt.contains(gp.indexOf(dMax))

      check1 && check2
    }
    p.check()
  }

  @Test def testPlToDosage() {
    val gt0 = Genotype.plToDosage(0, 20, 100)
    val gt1 = Genotype.plToDosage(20, 0, 100)
    val gt2 = Genotype.plToDosage(20, 100, 0)

    assert(D_==(gt0, 0.009900990296049406))
    assert(D_==(gt1, 0.9900990100009803))
    assert(D_==(gt2, 1.980198019704931))
  }
}
