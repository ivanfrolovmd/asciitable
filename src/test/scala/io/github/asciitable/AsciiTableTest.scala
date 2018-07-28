package io.github.asciitable

import org.scalatest.{FreeSpec, Matchers}

class AsciiTableTest extends FreeSpec with Matchers {
  "AsciiTable should" - {
    "render empty table" in {
      AsciiTable().toString shouldBe "<Empty>\n"
      AsciiTable().emptyMessage("Nothing").toString shouldBe "Nothing\n"
      AsciiTable().header("one", "two", "three").toString() shouldBe "<Empty>\n"
    }
    "should render simple table" in {
      AsciiTable().row("one").toString shouldBe
        """╔═══╗
          |║one║
          |╚═══╝
          |""".stripMargin
    }
    "should render simple table with header" in {
      AsciiTable()
        .header("first column", "2")
        .row("1", "second value")
        .row("", "")
        .toString shouldBe
        """╔════════════╤════════════╗
          |║first column│2           ║
          |╠════════════╪════════════╣
          |║1           │second value║
          |╟────────────┼────────────╢
          |║            │            ║
          |╚════════════╧════════════╝
          |""".stripMargin
    }

    "should render multiline cells" in {
      val charsS = Stream.iterate(0)(x => (x + 1) % 10)
      AsciiTable()
        .width(20)
        .row("1", charsS.take(200).mkString(""))
        .row("2", charsS.take(20).mkString(""))
        .toString shouldBe
        """╔═╤════════════════╗
          |║1│0123456789012345║
          |║ │6789012345678901║
          |║ │2345678901234567║
          |║ │8901234567890123║
          |║ │4567890123456789║
          |║ │0123456789012345║
          |║ │678901234567890…║
          |╟─┼────────────────╢
          |║2│0123456789012345║
          |║ │6789            ║
          |╚═╧════════════════╝
          |""".stripMargin
    }

    "should render clipped text in one-line cells" in {
      val charsS = Stream.iterate(0)(x => (x + 1) % 10)
      AsciiTable()
        .width(20)
        .multiline(false)
        .header("h", "value")
        .row("1", charsS.take(200).mkString(""))
        .row("2", "9876543210987654")
        .row()
        .row("34", charsS.take(20).mkString(""))
        .toString shouldBe
        """╔═╤════════════════╗
          |║h│value           ║
          |╠═╪════════════════╣
          |║1│012345678901234…║
          |║2│9876543210987654║
          |║ │                ║
          |║…│012345678901234…║
          |╚═╧════════════════╝
          |""".stripMargin
    }

    "should skip columns if they don't fit" in {
      val str = "12345"
      AsciiTable()
        .width(21)
        .columnMinWidth(5)
        .header("h1", "h2", "h3", "h4", "")
        .row(str, str * 2, str, str, "")
        .row()
        .row(str, str * 2, str, str, "")
        .row(str, str * 2, str, str, "")
        .toString shouldBe
        """╔═════╤═════╤═════╕→
          |║h1   │h2   │h3   │→
          |╠═════╪═════╪═════╡→
          |║12345│12345│12345│→
          |║     │12345│     │→
          |╟─────┼─────┼─────┤→
          |║     │     │     │→
          |╟─────┼─────┼─────┤→
          |║12345│12345│12345│→
          |║     │12345│     │→
          |╟─────┼─────┼─────┤→
          |║12345│12345│12345│→
          |║     │12345│     │→
          |╚═════╧═════╧═════╛→
          |""".stripMargin

      AsciiTable()
        .width(21)
        .columnMinWidth(5)
        .multiline(false)
        .header("h1", "h2", "h3", "h4", "")
        .row(str, str * 2, str, str, "")
        .row()
        .row(str, str * 2, str, str, "")
        .row(str, str * 2, str, str, "")
        .toString shouldBe
        """╔═════╤═════╤═════╕→
          |║h1   │h2   │h3   │→
          |╠═════╪═════╪═════╡→
          |║12345│1234…│12345│→
          |║     │     │     │→
          |║12345│1234…│12345│→
          |║12345│1234…│12345│→
          |╚═════╧═════╧═════╛→
          |""".stripMargin
    }

    "should render small columns when in the right side of the table" in {
      val str = "12345"
      AsciiTable()
        .width(15)
        .columnMinWidth(5)
        .header("h1", "h2", "h3", "h4", "h5")
        .row("0", str * 3, "1", "22", "3")
        .row("0", str * 3, "1", "22", "3")
        .row("0", str * 3, "1", "22", "3")
        .toString shouldBe
        """╔═╤═════╤═╤══╕→
          |║h│h2   │h│h4│→
          |║1│     │3│  │→
          |╠═╪═════╪═╪══╡→
          |║0│12345│1│22│→
          |║ │12345│ │  │→
          |║ │12345│ │  │→
          |╟─┼─────┼─┼──┤→
          |║0│12345│1│22│→
          |║ │12345│ │  │→
          |║ │12345│ │  │→
          |╟─┼─────┼─┼──┤→
          |║0│12345│1│22│→
          |║ │12345│ │  │→
          |║ │12345│ │  │→
          |╚═╧═════╧═╧══╛→
          |""".stripMargin
    }
  }
}
