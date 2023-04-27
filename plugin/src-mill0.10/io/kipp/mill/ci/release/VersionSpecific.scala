package io.kipp.mill.ci.release

import mill.main.EvaluatorScopt

private[release] object VersionSpecific {
  implicit def millScoptEvaluatorReads[A]: EvaluatorScopt[A] =
    new EvaluatorScopt[A]()
}
