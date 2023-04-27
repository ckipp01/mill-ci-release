package io.kipp.mill.ci.release

private[release] object VersionSpecific {
  implicit def millEvaluatorTokenReader =
    mill.main.TokenReaders.millEvaluatorTokenReader
}
