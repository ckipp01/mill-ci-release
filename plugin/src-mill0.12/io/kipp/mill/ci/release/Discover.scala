package io.kipp.mill.ci.release

private[release] object Discover {
  implicit def millEvaluatorTokenReader
      : mainargs.TokensReader[mill.eval.Evaluator] =
    mill.main.TokenReaders.millEvaluatorTokenReader
}
