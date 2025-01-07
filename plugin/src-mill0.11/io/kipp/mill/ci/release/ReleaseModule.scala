package io.kipp.mill.ci.release

object ReleaseModule extends GlobalReleaseModule {
  lazy val millDiscover: mill.define.Discover[this.type] =
    mill.define.Discover[this.type]
}
