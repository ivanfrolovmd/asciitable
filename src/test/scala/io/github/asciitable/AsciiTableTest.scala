package io.github.asciitable

import org.scalatest.{FreeSpec, Matchers}

class AsciiTableTest extends FreeSpec with Matchers {
  "AsciiTable should" - {
    "render empty table" in {
      AsciiTable().toString shouldBe "<Empty>\n"
      AsciiTable().header("one", "two", "three").toString() shouldBe "<Empty>\n"
    }
    "should render simple table" in {
      AsciiTable().row("one").toString shouldBe
        """┌───┐
          |│one│
          |└───┘
          |""".stripMargin
    }
    "should render simple table with header" in {
      val table = AsciiTable()
        .header("first column", "2")
        .row("1", "second value")
        .row("", "")

      table.toString shouldBe
        """┌────────────┬────────────┐
          |│first column│2           │
          |├────────────┼────────────┤
          |│1           │second value│
          |├────────────┼────────────┤
          |│            │            │
          |└────────────┴────────────┘
          |""".stripMargin
    }
  }
}
