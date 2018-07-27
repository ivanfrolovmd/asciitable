package io.github.asciitable

import java.io.{ByteArrayOutputStream, PrintWriter}

import io.github.asciitable.AsciiTable._

import scala.collection.immutable.Stream.StreamBuilder
import Maths._

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
    val widths              = calculateColumnSizes(width.getOrElse(DefaultWidth))
    val horizontalSeparator = renderSeparator(widths, Middle)

    pw.append(renderSeparator(widths, Top))

    header.foreach { head =>
      if (multiline) pw.append(renderMultiLineRow(widths)(head)) else pw.append(renderClippedRow(widths)(head))
      pw.append(horizontalSeparator)
    }

    val rowIt = rows.iterator
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
    val end                 = if (hasZeroWidthColumns) chars.arrowWithNewLine else chars.newLineString
    (row zip widths)
      .filter(_._2 > 0)
      .map { case (s, w) => s.padTo(w, chars.Blank) }
      .mkString(chars.VerticalLine.toString, chars.VerticalLine.toString, chars.VerticalLine + end)
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
    val end                 = if (hasZeroWidthColumns) chars.arrowWithNewLine else chars.newLineString
    widths.filter(_ > 0).map(w => "".padTo(w, chars.HorizontalLine)).mkString(cc._1, cc._2, cc._3 + end)
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

      (proportions zip maximums)
        .foldLeft((Seq.empty[Int], availableWidth, 1.0)) {
          case ((ws, avWidth, avRatio), (colRatio, colMax)) =>
            val proportionalWidth = if (avRatio > 0) Math.ceil(avWidth * colRatio / avRatio).toInt else 0
            val minWidth          = columnMinWidth min colMax
            val actualWidth       = proportionalWidth max minWidth
            if (actualWidth <= avWidth && avWidth > 0)
              (ws :+ actualWidth, avWidth - actualWidth, avRatio - colRatio)
            else
              (ws :+ 0, 0, 0)
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
    val CornerCharacters: Position => (String, String, String)
    val VerticalLine: Char
    val HorizontalLine: Char
    val NewLine: Char
    val Ellipsis: Char
    val Blank: Char
    val Arrow: Char
    lazy val newLineString    = s"$NewLine"
    lazy val arrowWithNewLine = s"$Arrow$NewLine"
  }
  private object Ascii extends CharacterSet {
    val CornerCharacters: Position => (String, String, String) = {
      case Top    => ("┌", "┬", "┐")
      case Middle => ("├", "┼", "┤")
      case Bottom => ("└", "┴", "┘")
    }
    val VerticalLine   = '│'
    val HorizontalLine = '─'
    val NewLine        = '\n'
    val Ellipsis       = '»'
    val Blank          = ' '
    val Arrow          = '░'
  }
  private object Unicode extends CharacterSet {
    val CornerCharacters: Position => (String, String, String) = {
      case Top    => ("┌", "┬", "┐")
      case Middle => ("├", "┼", "┤")
      case Bottom => ("└", "┴", "┘")
    }
    val VerticalLine   = '│'
    val HorizontalLine = '─'
    val NewLine        = '\n'
    val Ellipsis       = '…'
    val Blank          = ' '
    val Arrow          = '→'
  }

  private sealed trait Position
  private case object Top    extends Position
  private case object Middle extends Position
  private case object Bottom extends Position

  private val DefaultWidth           = 80
  private val DefaultRowMaxHeight    = 7
  private val DefaultColumnMinWidth  = 1
  private val DefaultColumnMaxWidth  = 5
  private val DefaultWidthPercentile = .75
  private val DefaultEmptyMessage    = "<Empty>"
  private val DefaultSampleRows      = 50
}
