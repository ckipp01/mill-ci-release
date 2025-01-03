package io.kipp.mill.ci.release

import mill._

object ReleaseModule extends GlobalReleaseModule {
  lazy val millDiscover: mill.define.Discover =
    mill.define.Discover[this.type]
}
