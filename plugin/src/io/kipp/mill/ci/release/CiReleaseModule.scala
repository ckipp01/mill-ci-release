package io.kipp.mill.ci.release

import de.tobiasroeser.mill.vcs.version.VcsVersion
import mill._
import mill.api.Result
import mill.define.ExternalModule
import mill.define.Task
import mill.eval.Evaluator
import mill.main.EvaluatorScopt
import mill.main.Tasks
import mill.scalalib.PublishModule
import mill.scalalib.publish.Artifact
import mill.scalalib.publish.SonatypePublisher

/** Helper module extending PublishModule. We use our own Trait to have a bit
  * more control over things and so that we can set the version for example for
  * the user. This should hopefully just be one less thing they need to worry
  * about. The entire goal of this is to make it frictionless for a user to
  * release their project.
  */
trait CiReleaseModule extends PublishModule {
  override def publishVersion: T[String] = T {
    val state = VcsVersion.vcsState()
    // Right now we sort of hack -SNAPSHOT on top of here.
    // https://github.com/lefou/mill-vcs-version/discussions/62
    val suffix =
      if (state.commitsSinceLastTag == 0) ""
      else "-SNAPSHOT"
    state.format() + suffix
  }
}

object ReleaseModule extends ExternalModule {

  /** This is a replacement for the mill.scalalib.PublishModule/publishAll task
    * that should basically work identically _but_ without requiring the user to
    * pass in anything. It also sets up your gpg stuff and grabs the necessary
    * env variables to publish to sonatype for you.
    */
  def publishAll(ev: Evaluator) = T.command {
    val log = T.log
    setupGpg()()
    val env = envTask()

    val modules = releaseModules(ev).map { m =>
      (m.sonatypeUri, m.sonatypeSnapshotUri)
    }

    val sonatypeUris = modules.map(_._1).toSet
    val sonatypeSnapshotUris = modules.map(_._2).toSet

    def mustBeUniqueMsg(value: String, values: Set[String]): String = {
      s"""It looks like you have multiple different values set for ${value}
           |
           |${values.mkString(" - ", " - \n", "")}
           |
           |In order to use publishAll these should all be the same.""".stripMargin
    }

    val result: Unit = if (sonatypeUris.size != 1) {
      Result.Failure[Unit](mustBeUniqueMsg("sonatypeUri", sonatypeUris))
    } else if (sonatypeSnapshotUris.size != 1) {
      Result.Failure[Unit](
        mustBeUniqueMsg("sonatypeSnapshotUri", sonatypeSnapshotUris)
      )
    } else {
      // Not ideal here to call head but we just checked up above and already failed
      // if they aren't size 1.
      val sonatypeUri = sonatypeUris.head
      val sonatypeSnapshotUri = sonatypeSnapshotUris.head

      if (env.isTag) {
        log.info("Tag push detected, publishing a new stable release")
        log.info(s"Publishing to ${sonatypeUri}")
      } else {
        log.info("No new tag detected, publishing a SNAPSHOT")
        log.info(s"Publishing to ${sonatypeSnapshotUri}")
      }

      // At this point since we pretty much have everything we need we mimic publishAll from here:
      // https://github.com/com-lihaoyi/mill/blob/d944b3cf2aa9a286262e7963a7fea63e1986c627/scalalib/src/PublishModule.scala#L214-L245
      val artifactPaths: Seq[(Seq[(os.Path, String)], Artifact)] =
        T.sequence(artifacts(ev).value)().map {
          case PublishModule.PublishData(a, s) =>
            (s.map { case (p, f) => (p.path, f) }, a)
        }

      new SonatypePublisher(
        sonatypeUri,
        sonatypeSnapshotUri,
        env.sonatypeCreds,
        signed = true,
        Seq(
          s"--passphrase=${env.pgpPassword}",
          "--no-tty",
          "--pinentry-mode",
          "loopback",
          "--batch",
          "--yes",
          "--armor",
          "--detach-sign"
        ),
        // TODO look at some of the larger Mill projects, do they all just use the defaults,
        // or should we make the defaults here a bit longer?
        readTimeout = 60000,
        connectTimeout = 5000,
        log,
        awaitTimeout = 120 * 1000,
        stagingRelease = true
      ).publishAll(
        release = true,
        artifactPaths: _*
      )
    }
    result
  }

  /** All the publish artifacts for the release modules.
    */
  private def artifacts(ev: Evaluator) = {
    val modules = releaseModules(ev).map { m => m.publishArtifacts }
    Tasks(modules)
  }

  private val envTask: Task[Env] = setupEnv()

  /** Ensures that your key is imported prio to signing and publishing.
    */
  private def setupGpg() = T.task {
    T.log.info("Attempting to setup gpg")
    val versionCall = os.proc("gpg", "--version").call()
    if (versionCall.exitCode != 0) {
      Result.Failure(
        "Unable to call gpg. Are you sure it's installed and set up correctly?"
      )
    } else {
      val echo = os.proc("echo", envTask().pgpSecret).spawn()
      val decoded = os.proc("base64", "--decode").spawn(stdin = echo.stdout)

      // https://dev.gnupg.org/T2313
      val imported = os
        .proc("gpg", "--batch", "--import", "--no-tty")
        .call(stdin = decoded.stdout)

      if (imported.exitCode != 0)
        Result.Failure(
          "Unable to import your pgp key. Make sure your secret is correct."
        )
      else ()
    }
  }

  /** Ensures that the user has all the ENV variable set up that are necessary
    * to both take care of pgp related stuff and also publish to sonatype.
    * @return
    *   a Env Task
    */
  private def setupEnv(): Task[Env] = T.input {
    val env = T.ctx().env
    val pgpSecret = env.get("PGP_SECRET")
    val pgpPassword = env.get("PGP_PASSPHRASE")
    val isTag = env.get("GITHUB_REF").exists(_.startsWith("refs/tags"))
    val sonatypeUser = env.get("SONATYPE_USERNAME")
    val sonatypePassword = env.get("SONATYPE_PASSWORD")

    if (pgpSecret.isEmpty) {
      Result.Failure("Missing PGP_SECRET. Make sure you have it set.")
    } else if (pgpPassword.isEmpty) {
      Result.Failure("Missing PGP_PASSPHRASE. Make sure you have it set.")
    } else if (sonatypeUser.isEmpty) {
      Result.Failure("Missing SONATYPE_USERNAME. Make sure you have it set.")
    } else if (sonatypePassword.isEmpty) {
      Result.Failure("Missing SONATYPE_PASSWORD. Make sure you have it set.")
    } else {
      Env(
        pgpSecret.get,
        pgpPassword.get,
        isTag,
        sonatypeUser.get,
        sonatypePassword.get
      )
    }
  }

  /** Gathers all the CiReleaseModules, which is used to determine what should
    * be released
    */
  private def releaseModules(ev: Evaluator) =
    ev.rootModule.millInternal.modules.collect { case m: CiReleaseModule => m }

  implicit def millScoptEvaluatorReads[A]: EvaluatorScopt[A] =
    new mill.main.EvaluatorScopt[A]()

  lazy val millDiscover = mill.define.Discover[this.type]
}
