import $ivy.`com.goyeau::mill-scalafix::0.3.1`
import $ivy.`io.chris-kipp::mill-ci-release::0.1.9`

import mill._
import mill.scalalib._
import mill.scalalib.scalafmt._
import mill.scalalib.publish._
import mill.scalalib.api.ZincWorkerUtil
import mill.scalalib.api.ZincWorkerUtil._

import com.goyeau.mill.scalafix.ScalafixModule
import de.tobiasroeser.mill.vcs.version.VcsVersion
import io.kipp.mill.ci.release.CiReleaseModule
import io.kipp.mill.ci.release.SonatypeHost

val millVersions = Seq("0.10.13", "0.11.6")
val millBinaryVersions = millVersions.map(scalaNativeBinaryVersion)
val scala213 = "2.13.12"
val pluginName = "mill-ci-release"

def millBinaryVersion(millVersion: String) = scalaNativeBinaryVersion(
  millVersion
)

def millVersion(binaryVersion: String) =
  millVersions.find(v => millBinaryVersion(v) == binaryVersion).get

object plugin extends Cross[Plugin](millBinaryVersions)
trait Plugin
    extends Cross.Module[String]
    with ScalaModule
    with CiReleaseModule
    with ScalafixModule
    with ScalafmtModule {

  override def scalaVersion = scala213

  override def artifactName =
    s"${pluginName}_mill${crossValue}"

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

  override def sonatypeHost = Some(SonatypeHost.s01)

  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-scalalib:${millVersion(crossValue)}"
  )
  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"de.tototec::de.tobiasroeser.mill.vcs.version_mill${crossValue}::0.4.0"
  )
  override def scalacOptions = Seq("-Ywarn-unused", "-deprecation")

  override def sources = T.sources {
    super.sources() ++ Seq(
      millSourcePath / s"src-mill${millVersion(crossValue).split('.').take(2).mkString(".")}"
    ).map(PathRef(_))
  }

  override def scalafixScalaBinaryVersion =
    ZincWorkerUtil.scalaBinaryVersion(scala213)
}
