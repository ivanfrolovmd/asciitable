package io.github.asciitable

object Maths {

  implicit class RichIntSeq(as: Array[Int]) {
    private lazy val sortedAs = as.sorted

    def median(): Option[Int] = percentile(.5)
    def percentile(p: Double): Option[Int] = sortedAs match {
      case Array()    => None
      case Array(one) => Some(one)
      case seq =>
        require(p >= 0 && p <= 1)
        val ix = Math.round((seq.length - 1) * p).toInt
        Some(seq.apply(ix))
    }
  }
}
