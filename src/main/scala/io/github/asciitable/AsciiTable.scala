package io.github.asciitable

import java.io.{ByteArrayOutputStream, PrintWriter, StringWriter}
import java.lang.Math.{max, min}

import scala.collection.immutable.Stream.StreamBuilder
import AsciiTable._

class AsciiTable {
  private var header: Option[Seq[String]] = None
  private val streamBuilder               = new StreamBuilder[Seq[String]]
  private var width: Option[Int]          = None

  def header(columnNames: String*): AsciiTable                         = { header = Some(columnNames); this }
  def header(columnNames: TraversableOnce[String]): AsciiTable         = { header = Some(columnNames.toSeq); this }
  def row(values: String*): AsciiTable                                 = { streamBuilder += values; this }
  def row(values: TraversableOnce[String]): AsciiTable                 = { streamBuilder += values.toSeq; this }
  def rows(rows: TraversableOnce[TraversableOnce[String]]): AsciiTable = { streamBuilder ++= rows.map(_.toSeq); this }
  def width(value: Int): AsciiTable                                    = { width = Some(value); this }

  private lazy val rows = streamBuilder.result()

  override def toString: String = {
    val boas = new ByteArrayOutputStream()
    write(boas)
    boas.toString
  }

  def write(out: java.io.OutputStream = System.out): Unit = out.synchronized {
    val w = new PrintWriter(out)
    if (rows.isEmpty) {
      w.println("<Empty>")
    } else {
      val widths = calculateColumnSizes(width.getOrElse(DefaultWidth))

      renderBorder(w, widths, Top)

      header.foreach(renderMultiLineRow(w, widths))
      header.foreach { _ =>
        renderBorder(w, widths)
      }

      val rowIt = rows.iterator
      while (rowIt.hasNext) {
        renderMultiLineRow(w, widths)(rowIt.next())
        if (rowIt.hasNext) renderBorder(w, widths)
      }

      renderBorder(w, widths, Bottom)
    }
    w.flush()
  }

  private def renderOneLineRow(str: PrintWriter, widths: List[Int])(row: Seq[String]) = {
    str.append('│')
    str.append(row.zip(widths).filter(_._2 > 0).map(trimPad.tupled).mkString("│"))
    str.append("│\n")
  }

  private def renderMultiLineRow(str: PrintWriter, widths: List[Int])(row: Seq[String]): Unit = {
    val cells    = row.zip(widths).filter(_._2 > 0).map(splitIntoLines.tupled)
    val height   = max(1, min(if (cells.isEmpty) 0 else cells.map(_.size).max, 7))
    val rowLines = cells.map(cell => cell.take(height) ++ List.fill(height - cell.size)("")).transpose
    rowLines.foreach(renderOneLineRow(str, widths.filter(_ > 0)))
  }

  private def renderBorder(out: PrintWriter, widths: List[Int], position: Position = Middle) = {
    out.append(CornerCharacters(position)._1)
    out.append(
      widths.filter(_ > 0).map(w => Seq.fill(w)('─').mkString("")).mkString(CornerCharacters(position)._2.toString))
    out.append(CornerCharacters(position)._3)
    out.append("\n")
  }

  private def calculateColumnSizes(width: Int): List[Int] = {
    val sizeMatrix       = rows.take(50).map(_.map(_.length).toArray).toArray.transpose
    val maximums         = sizeMatrix.map(_.max)
    val maximumsCombined = maximums.sum + maximums.length + 1
    if (maximumsCombined <= width) {
      maximums.toList
      header.fold(maximums.toList) { header =>
        (maximums zip header.map(_.length))
          .foldLeft((width - maximumsCombined, List.empty[Int])) {
            case ((rem, list), (cMax, hWidth)) =>
              if (hWidth > cMax) {
                val delta = Math.min(hWidth - cMax, rem)
                (rem - delta, list :+ (cMax + delta))
              } else (rem, list :+ cMax)
          }
          ._2
      }
    } else {
      val means = sizeMatrix.map(xs => xs.sum / xs.length)
      val mins  = sizeMatrix.map(_.min).map(s => min(max(s, 1), 5))

      val meansSum       = means.sum
      val proportions    = means.map(m => m.toDouble / meansSum)
      val availableWidth = width - means.length - 1

      proportions
        .zip(mins)
        .zipWithIndex
        .sortBy(_._1._1)
        .foldLeft(Seq[(Int, Int)](), availableWidth, 1.0) {
          case ((list, avW, propsum), ((prop, min), ix)) =>
            val width = max(min, (avW * prop / propsum).toInt)
            if (avW - width >= 0)
              (list :+ (width, ix), avW - width, propsum - prop)
            else
              (list :+ (0, ix), -1, propsum - prop)

        }
        ._1
        .sortBy(_._2)
        .map(_._1)
        .toList
    }
  }

  private val trimPad = (str: String, width: Int) =>
    if (width > 0) {
      val strBuilder = new StringBuilder(str)
      str.length to width foreach { _ =>
        strBuilder.append(' ')
      }
      strBuilder.length = width
      if (str.length > width && width > 0) strBuilder.setCharAt(width - 1, '…')
      strBuilder.toString()
    } else {
      ""
  }

  private val splitIntoLines = (str: String, width: Int) =>
    if (width > 0)
      str.grouped(width).toList
    else
      List()

}

object AsciiTable {
  def apply(): AsciiTable = new AsciiTable()

  case object Null { override def toString: String = "NULL" /* TODO ansi here */ }

  private val CornerCharacters = Map[Position, (Char, Char, Char)](
    Top    -> ('┌', '┬', '┐'),
    Middle -> ('├', '┼', '┤'),
    Bottom -> ('└', '┴', '┘')
  )
  private sealed trait Position
  private case object Top    extends Position
  private case object Middle extends Position
  private case object Bottom extends Position

  private val DefaultWidth = 80
}
