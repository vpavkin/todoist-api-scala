package ru.pavkin.todoist.api.core.model

import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}

class LabelColorSpec extends FunSuite with Matchers with GeneratorDrivenPropertyChecks {

  test("LabelColor.unsafeBy returns color if it exists") {
    forAll(Gen.choose(0, 12)) { (n: Int) =>
      whenever(n >= 0 && n <= 12) {
        noException should be thrownBy LabelColor.unsafeBy(n)
        LabelColor.unsafeBy(n).isPremium shouldBe n >= 8
      }
    }
    forAll { (n: Int) =>
      whenever(n < 0 || n > 12) {
        an[Exception] should be thrownBy LabelColor.unsafeBy(n)
      }
    }
  }
}

