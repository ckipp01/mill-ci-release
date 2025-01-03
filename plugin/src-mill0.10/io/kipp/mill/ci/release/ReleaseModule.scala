package io.kipp.mill.ci.release

object ReleaseModule extends GlobalReleaseModule {
  implicit val reader: mainargs.ArgReader[mill.eval.Evaluator] = ???
  lazy val millDiscover: mill.define.Discover[this.type] =
    mill.define.Discover[this.type]
}
