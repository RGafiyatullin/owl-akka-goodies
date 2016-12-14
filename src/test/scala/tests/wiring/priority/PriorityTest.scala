package tests.wiring.priority

import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.wiring.Priority
import org.scalatest.{FlatSpec, Matchers}

class PriorityTest extends FlatSpec with Matchers {
  import Priority._

  val simplePriorities: Seq[SimplePriority] = Seq(First, Early, Normal, Late, Last)

  val compoundPrioritiesSequences: Seq[Seq[Priority]] =
    simplePriorities.map( sp => Seq(Before(Before(sp)), Before(sp), sp, After(sp), After(After(sp))) ) ++
      simplePriorities.combinations(2).map {
        case Seq(lesser, greater) if lesser < greater =>
          Seq(Before(lesser), lesser, After(lesser), Before(greater), greater, After(greater))
        case _ =>
          Seq()
      }.toSeq


  "Reduction" should "reduce CompA(CompInvA(sp: SimplePriority)) into sp" in {
    for (sp <- simplePriorities) {
      After(Before(sp)).reduce should be(sp)
      Before(After(sp)).reduce should be(sp)
    }
  }

  it should "reduce CompB(CompA(CompInvA(sp: SimplePriority))) into CompB(sp)" in {
    for (sp <- simplePriorities) {
      After(After(Before(sp))).reduce should be(After(sp))
      After(Before(After(sp))).reduce should be(After(sp))

      Before(After(Before(sp))).reduce should be(Before(sp))
      Before(Before(After(sp))).reduce should be(Before(sp))
    }
  }

  "Base" should "work" in {
    for (sp <- simplePriorities) {
      sp.base should be(sp)
      After(sp).base should be(sp)
      Before(sp).base should be(sp)
    }
  }

  "Simple priorities" should "have order defined consistently" in {
    simplePriorities.permutations.foreach(_.sorted[Priority] should be (simplePriorities))
  }

  "Compound priorities" should "have order defined consistently" in {
    compoundPrioritiesSequences.foreach { cps =>
      println("CPS: %s".format(cps))
      cps.permutations.foreach(_.sorted should be (cps))
    }
  }
}
