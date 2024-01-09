package io.kipp.mill.ci.release

import mill.eval.Evaluator

private[release] object Eval {

  def evalOrThrow(ev: Evaluator): Evaluator.EvalOrThrow =
    Evaluator.evalOrThrow(ev)

}
