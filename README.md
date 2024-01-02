# mill-ci-release

This is a [Mill][mill] plugin modeled after the fantastic
[sbt-ci-release][sbt-ci-release] plugin which helps automate publishing to
[Sonatype][sonatype] from GitHub Actions with as little friction as possible.
These are the key features of using the plugin.

  - A new git tag is published as a regular release
  - A merge is published as a SNAPSHOT release
  - Auto versioning based on git by [mill-vcs-version][mill-vcs-version]
  - A simple one-liner in CI to publish

## Getting Started

If you've never published to Sonatype before you'll need to do a one-time setup
per domain name that you're publishing under. You can find the instructions for
this [here][sonatype-setup]. If you don't have a domain name you can use
`io.github.<@your_username>`.

**NOTE**: Keep in mind that as of February 2021 newly created accounts and group ids (even if your account was created before February 2021) are tied
to https://s01.oss.sonatype.org/ whereas older accounts will be tied to
https://oss.sonatype.org/. This matters when logging in. You'll also want to
make sure you set `sonatypeHost` to `Some(SonatypeHost.s01)` in this scenario. See [this section](#im-getting-a-403-when-attempting-to-publish-and-i-have-my-env-variables-correct)

### Installing the Plugin

To start using this plugin you'll want to include the following import in your
build file:

```scala
import $ivy.`io.chris-kipp::mill-ci-release::<latest-version>`
```

This plugin under the hood uses [mill-vcs-version][mill-vcs-version] to manage
your version, so if you have a `publishVersion` set, remove it. The reason for
this is that mill-ci-release is making sure that when you're on a snapshot
version, it's appending `-SNAPSHOT` which is necessary to publish to Sonatype
Snapshots. You still can override `publishVersion` locally, but then you're 100%
on your own to ensure that `-SNAPSHOT` is appended when necessary. This might
just be easily included in the plugin in the [future][mill-vcs-discussion].

The only other thing you'll need to do to your build is replace `PublishModule`
with `CiReleaseModule`.

```diff
- import de.tobiasroeser.mill.vcs.version.VcsVersion
+ import io.kipp.mill.ci.release.CiReleaseModule

object example 
    extends ScalaModule
-    with PublishModule {
+    with CiReleaseModule {

-  def publishVersion = VcsVersion.vcsState().format()
```

You'll still need to ensure your `pomSettings` are correctly filled in, just as
if you were extending `PublishModule`.

**NOTE**: Again, if you have a newly created account (as of February 2021)
you'll also want to ensure you add the following:

```diff
+ import io.kipp.mill.ci.release.SonatypeHost
...
+  override def sonatypeHost = Some(SonatypeHost.s01)
```

This will then set the correct `sonatypeUri` and `sonatypeSnapshotUri` for you.
If you have an older account, then there is no need to change the default or use
`sonatypeHost` at all.

### Using with custom Sonatype Nexus instances

If you're using your own instance of Sonatype Nexus, your configuration needs to be adapted slightly:
```diff
-  override def sonatypeHost = Some(SonatypeHost.s01)
+  override def sonatypeUri = "https://your-sonatype-nexus.url/path/to/releases"
+  override def sonatypeSnapshotUri = "https://your-sonatype-nexus.url/path/to/snapshots"
+
+  // The Open Source version of Nexus does not support staging
+  override def stagingRelease = false
```

### GPG

If you've never created a keypair before that can be used to sign your artifacts
you'll need to do this. You can find a guide for doing this [here][gpg].

### Secrets

Before using mill-ci-release in your GitHub actions workflow you'll need to have
the following secrets defined. You can add these by going to repo `Settings ->
Secrets -> New repository secret`.

Here are the necessary secrets:

- `PGP_PASSPHRASE`: The passphrase that was used when creating your keypair.
    This will be the same passphrase that you're prompted to use when copying
    the value for your `PGP_SECRET`.
- `PGP_SECRET`: A base64 encoded secret of your private key that you can export
    from the command line like below:

```
# macOS
gpg --export-secret-key -a $LONG_ID | base64 | pbcopy
# Ubuntu (assuming GNU base64)
gpg --export-secret-key -a $LONG_ID | base64 -w0 | xclip
# Arch
gpg --export-secret-key -a $LONG_ID | base64 | sed -z 's;\n;;g' | xclip -selection clipboard -i
# FreeBSD (assuming BSD base64)
gpg --export-secret-key -a $LONG_ID | base64 | xclip
# Windows
gpg --export-secret-key -a %LONG_ID% | openssl base64
```

- `SONATYPE_USERNAME`: The username you use to log into your Sonatype account.
- `SONATYPE_PASSWORD`: The password you use to log into your Sonatype account.

### GitHub Actions

You can use an exact copy of what is being used in this repo to publish by
doing the following command:

```
curl -sLo .github/workflows/release.yml --create-dirs https://raw.githubusercontent.com/ckipp01/mill-ci-release/main/.github/workflows/release.yml
```

This will create a `.github/workflows/release.yml` file which will be triggered
by a new git tag or a merge to `main`. The contents should look like this:

```yaml
name: Release
on:
  push:
    branches:
      - main
    tags: ["*"]
jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0
      - uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '17'
      - run: ./mill -i io.kipp.mill.ci.release.ReleaseModule/publishAll
        env:
          PGP_PASSPHRASE: ${{ secrets.PGP_PASSPHRASE }}
          PGP_SECRET: ${{ secrets.PGP_SECRET }}
          SONATYPE_PASSWORD: ${{ secrets.SONATYPE_PASSWORD }}
          SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
```

By default, this will publish all of your modules that are extending
`CiReleaseModule`.

## How does this differ from just using Mill?

The underlying publish should actually be identical as it's using the same
[Sonatype publisher][mill-publisher]. The difference is only in the set up.
Below is a comparison of what you'll commonly see with Mill to release vs using
mill-ci-release.

```diff
      - name: Publish
+       run: ./mill -i io.kipp.mill.ci.release.ReleaseModule/publishAll
-       run: |
-         if [[ $(git tag --points-at HEAD) != '' ]]; then
-           echo $PGP_PRIVATE_KEY | base64 --decode > gpg_key
-           gpg --import --no-tty --batch --yes gpg_key
-           rm gpg_key
-           ./mill -i mill.scalalib.PublishModule/publishAll \
-             --publishArtifacts __.publishArtifacts \
-             --sonatypeUri "https://s01.oss.sonatype.org/service/local" \
-             --sonatypeSnapshotUri "https://s01.oss.sonatype.org/content/repositories/snapshots" \
-             --sonatypeCreds $SONATYPE_USER:$SONATYPE_PASSWORD \
-             --gpgArgs --passphrase=$PGP_PASSWORD,--no-tty,--pinentry-mode,loopback,--batch,--yes,-a,-b \
-             --readTimeout 600000 \
-             --awaitTimeout 600000 \
-             --release true \
-             --signed true
-         fi
```

## FAQs

#### I'm getting a 403 when attempting to publish and I have my env variables correct

Most often this is due to not correctly setting the following if you have a new
account or group id:

```scala
override def sonatypeHost = Some(SonatypeHost.s01)
```

_Or manually doing_

```scala
override def sonatypeUri = "https://s01.oss.sonatype.org/service/local"
override def sonatypeSnapshotUri =
  "https://s01.oss.sonatype.org/content/repositories/snapshots"
```

#### It's publishing a 0.0.0-something-SNAPSHOT even though I have a git tag

If you see this it's probably because you forgot to add the `fetch-depth` in
checkout, meaning that no git tags are getting pulled in CI:

```yaml
- uses: actions/checkout@v3
  with:
    fetch-depth: 0
```

## Notes

This plugin has only really been tested on more minimal projects. There is
purposefully not many configuration options mainly because I firmly believe in
sane defaults that should easily allow what most users want to do with minimal
setup. If you're missing certain configuration options, please do open a
discussion or an issue and we can explore adding more customization options to
this.

[mill]: https://com-lihaoyi.github.io/mill/mill/Intro_to_Mill.html
[sbt-ci-release]: https://github.com/sbt/sbt-ci-release
[sonatype]: https://www.sonatype.com/
[mill-vcs-version]: https://github.com/lefou/mill-vcs-version
[sonatype-setup]: https://central.sonatype.org/pages/ossrh-guide.html
[mill-vcs-discussion]: https://github.com/lefou/mill-vcs-version/discussions/62
[gpg]: https://central.sonatype.org/publish/requirements/gpg/
[mill-publisher]: https://github.com/com-lihaoyi/mill/blob/main/scalalib/src/publish/SonatypePublisher.scala
