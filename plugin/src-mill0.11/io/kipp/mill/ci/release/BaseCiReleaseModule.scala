package io.kipp.mill.ci.release

import mill.Module

/** Helper module extending PublishModule. We use our own Trait to have a bit
  * more control over things and so that we can set the version for example for
  * the user. This should hopefully just be one less thing they need to worry
  * about. The entire goal of this is to make it frictionless for a user to
  * release their project.
  */
trait BaseCiReleaseModule extends Module {
  lazy val millDiscover: mill.define.Discover[this.type] =
    mill.define.Discover[this.type]
}
