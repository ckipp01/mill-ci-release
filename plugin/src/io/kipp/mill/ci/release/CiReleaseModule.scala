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

  /** If true, the version will be published as a snapshot without the commits
    * after the last tag and commit id. This is useful for projects that want to
    * publish snapshots overwriting the same version. The snapshot is named with
    * the next minor version and the suffix -SNAPSHOT. Eg. 0.5-SNAPSHOT if the
    * last tag was 0.4.0.
    */
  def flatSnapshot: Boolean = false

  override def publishVersion: T[String] = T {
    if (flatSnapshot) {
      val isTag =
        T.ctx().env.get("GITHUB_REF").exists(_.startsWith("refs/tags"))
      val state = VcsVersion.vcsState()
      if (state.commitsSinceLastTag == 0 && isTag) {
        state.stripV(state.lastTag.get)
      } else {
        val v = if (state.lastTag.isEmpty) { Array("0", "0", "0") }
        else { state.stripV(state.lastTag.get).split('.') }
        s"${v(0)}.${(v(1).toInt) + 1}-SNAPSHOT"
      }
    } else {
      VcsVersion.vcsState().format(untaggedSuffix = "-SNAPSHOT")
    }
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
