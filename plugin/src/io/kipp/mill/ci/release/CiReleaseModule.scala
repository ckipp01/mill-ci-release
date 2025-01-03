package io.kipp.mill.ci.release

import mill._
import mill.scalalib.PublishModule
import de.tobiasroeser.mill.vcs.version.VcsVersion

/** Helper module extending PublishModule. We use our own Trait to have a bit
  * more control over things and so that we can set the version for example for
  * the user. This should hopefully just be one less thing they need to worry
  * about. The entire goal of this is to make it frictionless for a user to
  * release their project.
  */
trait CiReleaseModule extends PublishModule {
  override def publishVersion: T[String] = T {
    VcsVersion.vcsState().format(untaggedSuffix = "-SNAPSHOT")
  }

  /** Helper available to users be able to more easily use the new s01 and
    * future hosts for sonatype by just setting this.
    */
  def sonatypeHost: Option[SonatypeHost] = None

  override def sonatypeUri: String = sonatypeHost match {
    case Some(SonatypeHost.Legacy) => "https://oss.sonatype.org/service/local"
    case Some(SonatypeHost.s01) => "https://s01.oss.sonatype.org/service/local"
    case None                   => super.sonatypeUri
  }

  override def sonatypeSnapshotUri: String = sonatypeHost match {
    case Some(SonatypeHost.Legacy) =>
      "https://oss.sonatype.org/content/repositories/snapshots"
    case Some(SonatypeHost.s01) =>
      "https://s01.oss.sonatype.org/content/repositories/snapshots"
    case None => super.sonatypeSnapshotUri
  }

  def stagingRelease: Boolean = true
}
