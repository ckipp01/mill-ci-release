package io.kipp.mill.ci.release

final case class Env(
    pgpSecret: String,
    pgpPassword: String,
    isTag: Boolean,
    sonatypeUser: String,
    sonatypePassword: String
) {
  val sonatypeCreds = s"${sonatypeUser}:${sonatypePassword}"
}

object Env {
  implicit def rw: upickle.default.ReadWriter[Env] =
    upickle.default.macroRW
}
