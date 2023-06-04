package io.kipp.mill.ci.release

private[release] object Discover {
  implicit def millEvaluatorTokenReader =
    mill.main.TokenReaders.millEvaluatorTokenReader
}
