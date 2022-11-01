import $ivy.`com.goyeau::mill-scalafix::0.2.11`
import $ivy.`io.chris-kipp::mill-ci-release::0.1.2`

import mill._
import mill.scalalib._
import mill.scalalib.scalafmt._
import mill.scalalib.publish._
import mill.scalalib.api.ZincWorkerUtil
import mill.scalalib.api.Util.scalaNativeBinaryVersion

import com.goyeau.mill.scalafix.ScalafixModule
import de.tobiasroeser.mill.vcs.version.VcsVersion
import io.kipp.mill.ci.release.CiReleaseModule

val millVersion = "0.10.0"
val scala213 = "2.13.8"
val pluginName = "mill-ci-release"

def millBinaryVersion(millVersion: String) = scalaNativeBinaryVersion(
  millVersion
)

object plugin
    extends ScalaModule
    with CiReleaseModule
    with ScalafixModule
    with ScalafmtModule {

  override def scalaVersion = scala213

  override def artifactName =
    s"${pluginName}_mill${millBinaryVersion(millVersion)}"

  override def pomSettings = PomSettings(
    description =
      "A Mill plugin to automate Sonatype releases from GitHub Actions.",
    organization = "io.chris-kipp",
    url = "https://github.com/ckipp01/mill-ci-release",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl
      .github(owner = "ckipp01", repo = "mill-ci-release"),
    developers =
      Seq(Developer("ckipp01", "Chris Kipp", "https://github.com/ckipp01"))
  )

  override def sonatypeUri = "https://s01.oss.sonatype.org/service/local"
  override def sonatypeSnapshotUri =
    "https://s01.oss.sonatype.org/content/repositories/snapshots"

  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  )
  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"de.tototec::de.tobiasroeser.mill.vcs.version_mill0.10::0.3.0"
  )
  override def scalacOptions = Seq("-Ywarn-unused", "-deprecation")

  override def scalafixScalaBinaryVersion =
    ZincWorkerUtil.scalaBinaryVersion(scala213)

  override def scalafixIvyDeps = Agg(
    ivy"com.github.liancheng::organize-imports:0.6.0"
  )
}
