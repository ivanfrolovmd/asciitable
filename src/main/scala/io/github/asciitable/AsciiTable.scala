package io.github.asciitable

import java.io.{ByteArrayOutputStream, PrintWriter}

import io.github.asciitable.AsciiTable._
import io.github.asciitable.Maths._

import scala.collection.immutable.Stream.StreamBuilder
import scala.language.postfixOps

class AsciiTable {
  private var header: Option[Seq[String]] = None
  private val streamBuilder               = new StreamBuilder[Seq[String]]
  private var width: Option[Int]          = None
  private var multiline: Boolean          = true
  private var emptyMessage                = DefaultEmptyMessage
  private var rowMaxHeight                = DefaultRowMaxHeight
  private var columnMinWidth              = DefaultColumnMinWidth
  private var columnMaxWidth              = DefaultColumnMaxWidth
  private var sampleRows                  = DefaultSampleRows
  private var chars: CharacterSet         = Unicode

  def header(columnNames: String*): AsciiTable                         = { header = Some(columnNames); this }
  def header(columnNames: TraversableOnce[String]): AsciiTable         = { header = Some(columnNames.toSeq); this }
  def row(values: String*): AsciiTable                                 = { streamBuilder += values; this }
  def row(values: TraversableOnce[String]): AsciiTable                 = { streamBuilder += values.toSeq; this }
  def rows(rows: TraversableOnce[TraversableOnce[String]]): AsciiTable = { streamBuilder ++= rows.map(_.toSeq); this }
  def width(value: Int): AsciiTable                                    = { width = Some(value); this }
  def multiline(value: Boolean): AsciiTable                            = { multiline = value; this }
  def emptyMessage(value: String): AsciiTable                          = { emptyMessage = value; this }
  def rowMaxHeight(value: Int): AsciiTable                             = { rowMaxHeight = value; this }
  def columnMinWidth(value: Int): AsciiTable                           = { columnMinWidth = value; this }
  def columnMaxWidth(value: Int): AsciiTable                           = { columnMaxWidth = value; this }
  def sampleAtMostRows(value: Int): AsciiTable                         = { sampleRows = value; this }
  def useAscii(value: Boolean): AsciiTable                             = { chars = if (value) Ascii else Unicode; this }

  private lazy val rows = streamBuilder.result()

  override def toString: String = {
    val boas = new ByteArrayOutputStream()
    write(boas)
    boas.toString
  }

  def write(out: java.io.OutputStream = System.out): Unit = {
    val w = new PrintWriter(out)
    if (rows.isEmpty) writeEmpty(w) else writeTable(w)
    w.flush()
  }

  private def writeEmpty(pw: PrintWriter): Unit = pw.println(emptyMessage)

  private def writeTable(pw: PrintWriter): Unit = {
    val widths = calculateColumnSizes(width.getOrElse(DefaultWidth))

    pw.append(renderSeparator(widths, Top))

    header.foreach { head =>
      if (multiline) pw.append(renderMultiLineRow(widths)(head)) else pw.append(renderClippedRow(widths)(head))
      pw.append(renderSeparator(widths, HeaderBottom))
    }

    val rowIt               = rows.iterator
    val horizontalSeparator = renderSeparator(widths, Middle)
    while (rowIt.hasNext) {
      val currentRow = rowIt.next().padTo(widths.size, "")
      pw.append(
        if (multiline) renderMultiLineRow(widths)(currentRow)
        else renderClippedRow(widths)(currentRow)
      )
      if (rowIt.hasNext && multiline) pw.append(horizontalSeparator)
    }

    pw.append(renderSeparator(widths, Bottom))
  }

  private def renderOneLineRow(widths: Seq[Int])(row: Seq[String]): String = {
    val hasZeroWidthColumns = widths.contains(0)
    val end =
      if (hasZeroWidthColumns) chars.VerticalLine + chars.arrowWithNewLine
      else chars.DoubleVerticalLine + chars.newLineString
    (row zip widths)
      .filter(_._2 > 0)
      .map { case (s, w) => s.padTo(w, chars.Blank) }
      .mkString(chars.DoubleVerticalLine.toString, chars.VerticalLine.toString, end)
  }

  private def renderClippedRow(widths: Seq[Int])(row: Seq[String]): String = {
    val cells = (row zip widths).map {
      case (s, w) if s.length > w => s.take(w - 1).padTo(w, chars.Ellipsis)
      case (s, _)                 => s
    }
    renderOneLineRow(widths)(cells)
  }

  private def renderMultiLineRow(widths: Seq[Int])(row: Seq[String]): String = {
    val cells    = (row zip widths).map(splitIntoLines.tupled)
    val height   = 1 max cells.map(_.size).max
    val rowLines = cells.map(_.take(height).padTo(height, "")).transpose
    rowLines.map(renderOneLineRow(widths)).mkString
  }

  private def renderSeparator(widths: Seq[Int], position: Position): String = {
    val cc                  = chars.CornerCharacters(position)
    val hasZeroWidthColumns = widths.contains(0)
    val end                 = if (hasZeroWidthColumns) cc._4 + chars.arrowWithNewLine else cc._3 + chars.newLineString
    val hor                 = if (position == Middle) chars.HorizontalLine else chars.DoubleHorizontalLine
    widths.filter(_ > 0).map(w => "".padTo(w, hor)).mkString(cc._1, cc._2, end)
  }

  private def calculateColumnSizes(width: Int): Seq[Int] = {
    val sizeMatrix       = rows.take(sampleRows).map(_.map(_.length).toArray).toArray.transpose
    val maximums         = sizeMatrix.map(_.max).toSeq
    val maximumsCombined = maximums.sum + maximums.length + 1
    if (maximumsCombined <= width) {
      // table fits total width; adjust header heights to the remaining space
      header.fold(maximums) { header =>
        (maximums zip header.map(_.length))
          .foldLeft((Seq.empty[Int], width - maximumsCombined)) {
            case ((ws, rem), (cMax, hWidth)) =>
              if (hWidth > cMax) {
                val delta = Math.min(hWidth - cMax, rem)
                (ws :+ (cMax + delta), rem - delta)
              } else (ws :+ cMax, rem)
          }
          ._1
      }
    } else {
      val medians        = sizeMatrix.map(c => c.percentile(DefaultWidthPercentile).getOrElse(0))
      val mediansSum     = medians.sum
      val proportions    = medians.map(m => m.toDouble / mediansSum)
      val availableWidth = width - medians.length - 1

      (proportions zip maximums zipWithIndex)
        .sortBy(_._1._1) // sort by proportions ascending
        .foldLeft((Seq.empty[(Int, Int)], availableWidth, 1.0)) {
          // calculate actual apportioned widths
          case ((ws, avWidth, avRatio), ((colRatio, colMax), ix)) =>
            val proportionalWidth =
              if (avRatio > 0 && avWidth > 0) Math.floor(avWidth * colRatio / avRatio).toInt else 0
            val minWidth    = columnMinWidth min (1 max colMax)
            val actualWidth = proportionalWidth max minWidth
            (ws :+ (actualWidth, ix), avWidth - actualWidth, avRatio - colRatio)
        }
        ._1
        .sortBy(_._2) // sort by index
        .map(_._1)
        .foldLeft((Seq.empty[Int], availableWidth)) {
          // cutoff at maxWidth
          case ((ws, avWidth), colWidth) =>
            if (colWidth <= avWidth)
              (ws :+ colWidth, avWidth - colWidth)
            else
              (ws :+ 0, 0)
        }
        ._1
    }
  }

  private val splitIntoLines = (str: String, width: Int) => {
    if (width == 0) {
      Seq.empty[String]
    } else {
      val lines         = str.grouped(width).take(rowMaxHeight).toIndexedSeq
      val (top, bottom) = lines.splitAt(rowMaxHeight - 1)
      val bottomWithEllipsis =
        if (rowMaxHeight * width < str.length) bottom.map(_.take(width - 1).padTo(width, chars.Ellipsis))
        else bottom
      top ++ bottomWithEllipsis
    }
  }
}

object AsciiTable {
  def apply(): AsciiTable = new AsciiTable()

  private sealed trait CharacterSet {
    val CornerCharacters: Position => (String, String, String, String)
    val DoubleVerticalLine: Char
    val VerticalLine: Char
    val DoubleHorizontalLine: Char
    val HorizontalLine: Char
    val NewLine: Char
    val Ellipsis: Char
    val Blank: Char
    val Arrow: Char
    lazy val newLineString    = s"$NewLine"
    lazy val arrowWithNewLine = s"$Arrow$NewLine"
  }
  private object Ascii extends CharacterSet {
    val CornerCharacters: Position => (String, String, String, String) = {
      case Top          => ("+", "+", "+", "+")
      case HeaderBottom => ("+", "+", "+", "+")
      case Middle       => ("+", "+", "+", "+")
      case Bottom       => ("+", "+", "+", "+")
    }
    val DoubleVerticalLine   = '|'
    val VerticalLine         = '|'
    val DoubleHorizontalLine = '-'
    val HorizontalLine       = '-'
    val NewLine              = '\n'
    val Ellipsis             = '_'
    val Blank                = ' '
    val Arrow                = '@'
  }
  private object Unicode extends CharacterSet {
    val CornerCharacters: Position => (String, String, String, String) = {
      case Top          => ("╔", "╤", "╗", "╕")
      case HeaderBottom => ("╠", "╪", "╣", "╡")
      case Middle       => ("╟", "┼", "╢", "┤")
      case Bottom       => ("╚", "╧", "╝", "╛")
    }
    val DoubleVerticalLine   = '║'
    val VerticalLine         = '│'
    val DoubleHorizontalLine = '═'
    val HorizontalLine       = '─'
    val NewLine              = '\n'
    val Ellipsis             = '…'
    val Blank                = ' '
    val Arrow                = '→'
  }

  private sealed trait Position
  private case object Top          extends Position
  private case object HeaderBottom extends Position
  private case object Middle       extends Position
  private case object Bottom       extends Position

  private val DefaultWidth           = 80
  private val DefaultRowMaxHeight    = 7
  private val DefaultColumnMinWidth  = 1
  private val DefaultColumnMaxWidth  = 5
  private val DefaultWidthPercentile = .75
  private val DefaultEmptyMessage    = "<Empty>"
  private val DefaultSampleRows      = 50
}
