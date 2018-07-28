package io.github.asciitable

import java.io.{ByteArrayOutputStream, OutputStream}

import org.scalatest.{FreeSpec, Matchers}

class StreamingTest extends FreeSpec with Matchers {
  "Don't lose or repeat sampled rows" in {
    val baos        = new ByteArrayOutputStream()
    val rowIterator = new RowProvider(cnt => Seq(s"column1, row$cnt", s"column2, row$cnt", s"column3, row$cnt"))
    AsciiTable()
      .multiline(false)
      .sampleAtMostRows(5)
      .rows(rowIterator.take(10))
      .write(baos)

    baos.toString shouldBe
      """╔═════════════╤═════════════╤═════════════╗
        |║column1, row1│column2, row1│column3, row1║
        |║column1, row2│column2, row2│column3, row2║
        |║column1, row3│column2, row3│column3, row3║
        |║column1, row4│column2, row4│column3, row4║
        |║column1, row5│column2, row5│column3, row5║
        |║column1, row6│column2, row6│column3, row6║
        |║column1, row7│column2, row7│column3, row7║
        |║column1, row8│column2, row8│column3, row8║
        |║column1, row9│column2, row9│column3, row9║
        |║column1, row…│column2, row…│column3, row…║
        |╚═════════════╧═════════════╧═════════════╝
        |""".stripMargin
  }

  "Test large set" ignore {
    val devnull = new OutputStream {
      var size: Long                   = 0L
      override def write(b: Int): Unit = size += 1
    }
    val row         = Seq.fill(10)("1234567890" * 20)
    val rowProvider = new RowProvider(Function.const(row))
    AsciiTable()
      .multiline(false)
      .width(1000)
      .sampleAtMostRows(5)
      .rows(rowProvider.take(10000000))
      .write(devnull)
    devnull.size shouldBe 10420005996L
  }

  class RowProvider(row: Int => Seq[String]) extends Iterator[Seq[String]] {
    private var cnt               = 0
    override def hasNext: Boolean = true
    override def next(): Seq[String] = {
      cnt += 1
      row(cnt)
    }
  }
}
