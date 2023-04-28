package io.kipp.mill.ci.release

private[release] object Reader {
  implicit def millEvaluatorTokenReader =
    mill.main.TokenReaders.millEvaluatorTokenReader
}
