package io.kipp.mill.ci.release

import mainargs.TokensReader
import mill.eval.Evaluator

private[release] object Discover {
  implicit def millEvaluatorTokenReader: TokensReader[Evaluator] =
    mill.main.TokenReaders.millEvaluatorTokenReader
}
